package com.shuimumu.fuluogame.task;

import com.shuimumu.fuluogame.constants.PostRedisKeys;
import com.shuimumu.fuluogame.constants.UserRedisKeys;
import com.shuimumu.fuluogame.entity.Post;
import com.shuimumu.fuluogame.entity.Topic;
import com.shuimumu.fuluogame.entity.UserStat;
import com.shuimumu.fuluogame.mapper.PostMapper;
import com.shuimumu.fuluogame.mapper.TopicMapper;
import com.shuimumu.fuluogame.mapper.UserStatMapper;
import com.shuimumu.fuluogame.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class StatsSyncTask {

    private static final int BATCH_SIZE = 500;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserStatMapper userStatMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private PostMapper postMapper;

    @Scheduled(cron = "0 * * * * ?")
    public void syncDirty() {
        syncUserStats();
        syncPostCounts();
        syncTopics();
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void decayHotScores() {
        Set<String> members = stringRedisTemplate.opsForZSet()
                .reverseRange(PostRedisKeys.hotZset(), 0, -1);
        if (members == null || members.isEmpty()) return;
        for (String pid : members) {
            Double score = stringRedisTemplate.opsForZSet().score(PostRedisKeys.hotZset(), pid);
            if (score != null && score > 0.01) {
                stringRedisTemplate.opsForZSet().add(PostRedisKeys.hotZset(), pid, score * 0.75);
            }
        }
    }

    /**
     * 清理活跃用户 ZSET 中超过 ACTIVE_WINDOW_DAYS 不开活跃的用户。
     * 4:17 执行，错开 4:00 的 decayHotScores 与整点 syncDirty，规避串行争用。
     */
    @Scheduled(cron = "0 17 4 * * ?")
    public void cleanInactiveUsers() {
        long cutoff = System.currentTimeMillis() / 1000 - UserRedisKeys.ACTIVE_WINDOW_SEC;
        stringRedisTemplate.opsForZSet().removeRangeByScore(
                UserRedisKeys.activeUsers(), 0, cutoff);
    }

    // ==================== 用户统计 → user_stat ====================

    private void syncUserStats() {
        List<UserStat> batch = new ArrayList<>();
        forEachDirty(UserRedisKeys.statsDirty(), userIdStr -> {
            Long userId = Long.parseLong(userIdStr);
            Map<Object, Object> map = stringRedisTemplate.opsForHash()
                    .entries(UserRedisKeys.stats(userId));
            if (map.isEmpty()) return;

            UserStat stat = new UserStat();
            stat.setUserId(userId);
            stat.setPosts(toLong(map.get("posts")));
            stat.setFollowing(toLong(map.get("following")));
            stat.setFollowers(toLong(map.get("followers")));
            stat.setLikes(toLong(map.get("likes")));
            batch.add(stat);

            if (stat.getFollowers() >= 1000) {
                stringRedisTemplate.opsForSet().add(UserRedisKeys.bigV(), userIdStr);
            } else {
                stringRedisTemplate.opsForSet().remove(UserRedisKeys.bigV(), userIdStr);
            }
        }, () -> {
            userStatMapper.upsertBatch(batch);
            batch.clear();
        });
    }

    // ==================== 帖子计数 → post 表 ====================

    private void syncPostCounts() {
        List<Post> batch = new ArrayList<>();
        forEachDirty(PostRedisKeys.dirty(), postIdStr -> {
            Long postId = Long.parseLong(postIdStr);
            Map<Object, Object> map = stringRedisTemplate.opsForHash()
                    .entries(PostRedisKeys.counts(postId));
            if (map.isEmpty()) return;

            Post post = new Post();
            post.setId(postId);
            post.setLikes((int) toLong(map.get("likes")));
            post.setComments((int) toLong(map.get("comments")));
            post.setViews((int) toLong(map.get("views")));
            batch.add(post);
        }, () -> {
            postMapper.upsertBatch(batch);
            batch.clear();
        });
    }

    // ==================== 话题统计 → topic 表 ====================

    private void syncTopics() {
        List<Topic> batch = new ArrayList<>();
        forEachDirty(PostRedisKeys.topicDirty(), idStr -> {
            Long topicId = Long.parseLong(idStr);
            Map<Object, Object> map = stringRedisTemplate.opsForHash()
                    .entries(PostRedisKeys.topic(topicId));
            if (map.isEmpty()) return;

            Topic topic = new Topic();
            topic.setId(topicId);
            topic.setTotalViews(RedisUtils.toLong(map.get("views")));
            topic.setTotalPosts(RedisUtils.toLong(map.get("posts")));
            batch.add(topic);
        }, () -> {
            topicMapper.upsertBatch(batch);
            batch.clear();
        });
    }

    // ==================== 通用 ====================

    private void forEachDirty(String key, java.util.function.Consumer<String> handler,
                              Runnable flush) {
        while (true) {
            List<String> members = stringRedisTemplate.opsForSet().pop(key, BATCH_SIZE);
            if (members == null || members.isEmpty()) break;
            List<String> failed = new ArrayList<>();
            for (String member : members) {
                try {
                    handler.accept(member);
                } catch (Exception e) {
                    log.error("同步失败 {} {}", key, member, e);
                    failed.add(member);
                }
            }
            try {
                flush.run();
            } catch (Exception e) {
                log.error("flush失败 {} 回写{}条", key, members.size(), e);
                stringRedisTemplate.opsForSet().add(key, members.toArray(new String[0]));
                continue;
            }
            if (!failed.isEmpty()) {
                stringRedisTemplate.opsForSet().add(key, failed.toArray(new String[0]));
            }
        }
    }

    private long toLong(Object o) {
        return RedisUtils.toLong(o);
    }
}
