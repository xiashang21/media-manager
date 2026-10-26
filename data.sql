-- VideoHub seed data: 30 demo records with accessible public test videos.
-- REPLACE INTO makes this script safe to run again and updates existing demo rows.

USE video_db;

REPLACE INTO video
(id, title, description, file_name, original_name, url, cover_url, size, duration, format, category, tags, play_count, like_count, uploader, status, create_time, update_time, deleted)
VALUES
(1, 'Big Buck Bunny 演示短片 #01', 'W3Schools 提供的 Big Buck Bunny 测试视频，适合验证在线播放和拖动进度条。', 'demo_01.mp4', 'sample_01.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', NULL, 790541, 10, 'mp4', '动画', 'BigBuckBunny,动画,演示', 157, 49, 'admin', 1, NOW() - INTERVAL 1 DAY, NOW(), 0),
(2, 'Bear 动画短视频 #02', '轻量动画测试视频，适合验证列表、卡片、播放量和点赞功能。', 'demo_02.mp4', 'sample_02.mp4', 'https://www.w3schools.com/html/movie.mp4', NULL, 792589, 6, 'mp4', '动画', 'Bear,动画,短视频', 194, 68, 'demo', 1, NOW() - INTERVAL 2 DAY, NOW(), 0),
(3, 'Big Buck Bunny 完整版 #03', 'W3C 媒体测试视频，时长约 1 分钟，适合演示长视频播放和断点续播。', 'demo_03.mp4', 'sample_03.mp4', 'https://media.w3.org/2010/05/bunny/movie.mp4', NULL, 5517016, 60, 'mp4', '动画', 'BigBuckBunny,W3C,完整版', 231, 87, 'admin', 1, NOW() - INTERVAL 3 DAY, NOW(), 0),
(4, 'Big Buck Bunny 预告片 #04', 'W3C 提供的动画预告片，适合演示分类筛选和热门推荐。', 'demo_04.mp4', 'sample_04.mp4', 'https://media.w3.org/2010/05/bunny/trailer.mp4', NULL, 4208192, 33, 'mp4', '动画', '预告片,W3C', 268, 106, 'creator', 1, NOW() - INTERVAL 4 DAY, NOW(), 0),
(5, 'Ocean 海洋演示视频 #05', 'Video.js 官方示例视频，画面为海洋场景，适合演示多字段排序。', 'demo_05.mp4', 'sample_05.mp4', 'https://vjs.zencdn.net/v/oceans.mp4', NULL, 5521112, 46, 'mp4', '自然', '海洋,自然,VideoJS', 305, 125, 'traveler', 1, NOW() - INTERVAL 5 DAY, NOW(), 0),
(6, '5 秒短视频测试 #06', '5 秒短视频，适合快速验证上传、播放和交互接口。', 'demo_06.mp4', 'sample_06.mp4', 'https://download.samplelib.com/mp4/sample-5s.mp4', NULL, 2412288, 5, 'mp4', '短片', '5秒,测试,短视频', 342, 144, 'demo', 1, NOW() - INTERVAL 6 DAY, NOW(), 0),
(7, '10 秒短视频测试 #07', '10 秒短视频，适合验证文件大小排序和时长显示。', 'demo_07.mp4', 'sample_07.mp4', 'https://download.samplelib.com/mp4/sample-10s.mp4', NULL, 4214336, 10, 'mp4', '短片', '10秒,测试', 379, 163, 'tester', 1, NOW() - INTERVAL 7 DAY, NOW(), 0),
(8, '720P 1MB 视频测试 #08', '大小约 1MB 的 720P 测试视频，适合演示上传进度和文件大小排序。', 'demo_08.mp4', 'sample_08.mp4', 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4', NULL, 1064960, 10, 'mp4', '教程', '720P,1MB,测试', 416, 182, 'teacher', 1, NOW() - INTERVAL 8 DAY, NOW(), 0),
(9, '通用示例视频 #09', '通用 MP4 示例视频，适合演示搜索、分页和播放弹窗。', 'demo_09.mp4', 'sample_09.mp4', 'https://filesamples.com/samples/video/mp4/sample_640x360.mp4', NULL, 592432, 13, 'mp4', '通用', '640x360,示例', 453, 201, 'video_user', 1, NOW() - INTERVAL 9 DAY, NOW(), 0),
(10, 'Big Buck Bunny 演示短片 #10', 'W3Schools 提供的 Big Buck Bunny 测试视频，适合验证在线播放和拖动进度条。', 'demo_10.mp4', 'sample_10.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', NULL, 808973, 10, 'mp4', '动画', 'BigBuckBunny,动画,演示', 490, 220, 'admin', 1, NOW() - INTERVAL 10 DAY, NOW(), 0),
(11, 'Bear 动画短视频 #11', '轻量动画测试视频，适合验证列表、卡片、播放量和点赞功能。', 'demo_11.mp4', 'sample_11.mp4', 'https://www.w3schools.com/html/movie.mp4', NULL, 811021, 6, 'mp4', '动画', 'Bear,动画,短视频', 527, 239, 'demo', 1, NOW() - INTERVAL 11 DAY, NOW(), 0),
(12, 'Big Buck Bunny 完整版 #12', 'W3C 媒体测试视频，时长约 1 分钟，适合演示长视频播放和断点续播。', 'demo_12.mp4', 'sample_12.mp4', 'https://media.w3.org/2010/05/bunny/movie.mp4', NULL, 5535448, 60, 'mp4', '动画', 'BigBuckBunny,W3C,完整版', 564, 258, 'admin', 1, NOW() - INTERVAL 12 DAY, NOW(), 0),
(13, 'Big Buck Bunny 预告片 #13', 'W3C 提供的动画预告片，适合演示分类筛选和热门推荐。', 'demo_13.mp4', 'sample_13.mp4', 'https://media.w3.org/2010/05/bunny/trailer.mp4', NULL, 4226624, 33, 'mp4', '动画', '预告片,W3C', 601, 277, 'creator', 1, NOW() - INTERVAL 13 DAY, NOW(), 0),
(14, 'Ocean 海洋演示视频 #14', 'Video.js 官方示例视频，画面为海洋场景，适合演示多字段排序。', 'demo_14.mp4', 'sample_14.mp4', 'https://vjs.zencdn.net/v/oceans.mp4', NULL, 5539544, 46, 'mp4', '自然', '海洋,自然,VideoJS', 638, 296, 'traveler', 1, NOW() - INTERVAL 14 DAY, NOW(), 0),
(15, '5 秒短视频测试 #15', '5 秒短视频，适合快速验证上传、播放和交互接口。', 'demo_15.mp4', 'sample_15.mp4', 'https://download.samplelib.com/mp4/sample-5s.mp4', NULL, 2430720, 5, 'mp4', '短片', '5秒,测试,短视频', 675, 315, 'demo', 1, NOW() - INTERVAL 15 DAY, NOW(), 0),
(16, '10 秒短视频测试 #16', '10 秒短视频，适合验证文件大小排序和时长显示。', 'demo_16.mp4', 'sample_16.mp4', 'https://download.samplelib.com/mp4/sample-10s.mp4', NULL, 4232768, 10, 'mp4', '短片', '10秒,测试', 712, 334, 'tester', 1, NOW() - INTERVAL 16 DAY, NOW(), 0),
(17, '720P 1MB 视频测试 #17', '大小约 1MB 的 720P 测试视频，适合演示上传进度和文件大小排序。', 'demo_17.mp4', 'sample_17.mp4', 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4', NULL, 1083392, 10, 'mp4', '教程', '720P,1MB,测试', 749, 353, 'teacher', 1, NOW() - INTERVAL 17 DAY, NOW(), 0),
(18, '通用示例视频 #18', '通用 MP4 示例视频，适合演示搜索、分页和播放弹窗。', 'demo_18.mp4', 'sample_18.mp4', 'https://filesamples.com/samples/video/mp4/sample_640x360.mp4', NULL, 610864, 13, 'mp4', '通用', '640x360,示例', 786, 372, 'video_user', 1, NOW() - INTERVAL 18 DAY, NOW(), 0),
(19, 'Big Buck Bunny 演示短片 #19', 'W3Schools 提供的 Big Buck Bunny 测试视频，适合验证在线播放和拖动进度条。', 'demo_19.mp4', 'sample_19.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', NULL, 827405, 10, 'mp4', '动画', 'BigBuckBunny,动画,演示', 823, 391, 'admin', 1, NOW() - INTERVAL 19 DAY, NOW(), 0),
(20, 'Bear 动画短视频 #20', '轻量动画测试视频，适合验证列表、卡片、播放量和点赞功能。', 'demo_20.mp4', 'sample_20.mp4', 'https://www.w3schools.com/html/movie.mp4', NULL, 829453, 6, 'mp4', '动画', 'Bear,动画,短视频', 860, 410, 'demo', 1, NOW() - INTERVAL 20 DAY, NOW(), 0),
(21, 'Big Buck Bunny 完整版 #21', 'W3C 媒体测试视频，时长约 1 分钟，适合演示长视频播放和断点续播。', 'demo_21.mp4', 'sample_21.mp4', 'https://media.w3.org/2010/05/bunny/movie.mp4', NULL, 5553880, 60, 'mp4', '动画', 'BigBuckBunny,W3C,完整版', 897, 429, 'admin', 1, NOW() - INTERVAL 21 DAY, NOW(), 0),
(22, 'Big Buck Bunny 预告片 #22', 'W3C 提供的动画预告片，适合演示分类筛选和热门推荐。', 'demo_22.mp4', 'sample_22.mp4', 'https://media.w3.org/2010/05/bunny/trailer.mp4', NULL, 4245056, 33, 'mp4', '动画', '预告片,W3C', 934, 448, 'creator', 1, NOW() - INTERVAL 22 DAY, NOW(), 0),
(23, 'Ocean 海洋演示视频 #23', 'Video.js 官方示例视频，画面为海洋场景，适合演示多字段排序。', 'demo_23.mp4', 'sample_23.mp4', 'https://vjs.zencdn.net/v/oceans.mp4', NULL, 5557976, 46, 'mp4', '自然', '海洋,自然,VideoJS', 971, 467, 'traveler', 1, NOW() - INTERVAL 23 DAY, NOW(), 0),
(24, '5 秒短视频测试 #24', '5 秒短视频，适合快速验证上传、播放和交互接口。', 'demo_24.mp4', 'sample_24.mp4', 'https://download.samplelib.com/mp4/sample-5s.mp4', NULL, 2449152, 5, 'mp4', '短片', '5秒,测试,短视频', 1008, 486, 'demo', 1, NOW() - INTERVAL 24 DAY, NOW(), 0),
(25, '10 秒短视频测试 #25', '10 秒短视频，适合验证文件大小排序和时长显示。', 'demo_25.mp4', 'sample_25.mp4', 'https://download.samplelib.com/mp4/sample-10s.mp4', NULL, 4251200, 10, 'mp4', '短片', '10秒,测试', 1045, 505, 'tester', 1, NOW() - INTERVAL 25 DAY, NOW(), 0),
(26, '720P 1MB 视频测试 #26', '大小约 1MB 的 720P 测试视频，适合演示上传进度和文件大小排序。', 'demo_26.mp4', 'sample_26.mp4', 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4', NULL, 1101824, 10, 'mp4', '教程', '720P,1MB,测试', 1082, 524, 'teacher', 1, NOW() - INTERVAL 26 DAY, NOW(), 0),
(27, '通用示例视频 #27', '通用 MP4 示例视频，适合演示搜索、分页和播放弹窗。', 'demo_27.mp4', 'sample_27.mp4', 'https://filesamples.com/samples/video/mp4/sample_640x360.mp4', NULL, 629296, 13, 'mp4', '通用', '640x360,示例', 1119, 543, 'video_user', 1, NOW() - INTERVAL 27 DAY, NOW(), 0),
(28, 'Big Buck Bunny 演示短片 #28', 'W3Schools 提供的 Big Buck Bunny 测试视频，适合验证在线播放和拖动进度条。', 'demo_28.mp4', 'sample_28.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', NULL, 845837, 10, 'mp4', '动画', 'BigBuckBunny,动画,演示', 1156, 562, 'admin', 1, NOW() - INTERVAL 28 DAY, NOW(), 0),
(29, 'Bear 动画短视频 #29', '轻量动画测试视频，适合验证列表、卡片、播放量和点赞功能。', 'demo_29.mp4', 'sample_29.mp4', 'https://www.w3schools.com/html/movie.mp4', NULL, 847885, 6, 'mp4', '动画', 'Bear,动画,短视频', 1193, 581, 'demo', 1, NOW() - INTERVAL 29 DAY, NOW(), 0),
(30, 'Big Buck Bunny 完整版 #30', 'W3C 媒体测试视频，时长约 1 分钟，适合演示长视频播放和断点续播。', 'demo_30.mp4', 'sample_30.mp4', 'https://media.w3.org/2010/05/bunny/movie.mp4', NULL, 5572312, 60, 'mp4', '动画', 'BigBuckBunny,W3C,完整版', 1230, 600, 'admin', 1, NOW() - INTERVAL 30 DAY, NOW(), 0);

-- 为演示数据补充本地封面；重复执行不会覆盖用户自己上传的封面。
USE video_db;
UPDATE `video`
SET `cover_url` = CONCAT('/covers/demo-', MOD(`id` - 1, 6) + 1, '.svg')
WHERE `id` BETWEEN 1 AND 30 AND (`cover_url` IS NULL OR `cover_url` = '');
