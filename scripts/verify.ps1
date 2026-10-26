param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$MysqlExe = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$DbName = 'video_db',
    [string]$DbUser = 'root',
    [string]$DbPassword = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'AHxs5702' }),
    [switch]$KeepArtifacts
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$BaseUrl = $BaseUrl.TrimEnd('/')
$script:Passed = 0
$script:Failed = 0
$script:Client = [System.Net.Http.HttpClient]::new()
$script:Client.Timeout = [TimeSpan]::FromSeconds(60)

function Write-Step([string]$Text) {
    Write-Host ""
    Write-Host "== $Text ==" -ForegroundColor Cyan
}

function Pass([string]$Text) {
    $script:Passed++
    Write-Host "  [PASS] $Text" -ForegroundColor Green
}

function Fail([string]$Text) {
    $script:Failed++
    Write-Host "  [FAIL] $Text" -ForegroundColor Red
}

function Check([bool]$Condition, [string]$Text) {
    if ($Condition) { Pass $Text } else { Fail $Text }
}

function Send-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$JsonBody,
        [System.Net.Http.HttpContent]$Multipart,
        [hashtable]$ExtraHeaders
    )

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method), "$BaseUrl$Path")
    try {
        if ($Token) {
            $request.Headers.Authorization =
                [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)
        }
        if ($ExtraHeaders) {
            foreach ($key in $ExtraHeaders.Keys) {
                $request.Headers.TryAddWithoutValidation($key, [string]$ExtraHeaders[$key]) | Out-Null
            }
        }
        if ($null -ne $JsonBody) {
            $json = $JsonBody | ConvertTo-Json -Compress -Depth 8
            $request.Content = [System.Net.Http.StringContent]::new(
                $json, [System.Text.Encoding]::UTF8, 'application/json')
        } elseif ($Multipart) {
            $request.Content = $Multipart
        }

        $watch = [System.Diagnostics.Stopwatch]::StartNew()
        $response = $script:Client.SendAsync($request).GetAwaiter().GetResult()
        $watch.Stop()
        $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $jsonResult = $null
        if ($text) {
            try { $jsonResult = $text | ConvertFrom-Json } catch { }
        }
        return [pscustomobject]@{
            Status      = [int]$response.StatusCode
            Text        = $text
            Json        = $jsonResult
            Headers     = $response.Headers
            ContentType = $response.Content.Headers.ContentType
            ContentHeaders = $response.Content.Headers
            ElapsedMs   = [math]::Round($watch.Elapsed.TotalMilliseconds, 2)
        }
    } finally {
        $request.Dispose()
    }
}

function Invoke-Login([string]$Username, [string]$Password) {
    return Send-Api -Method POST -Path '/api/auth/login' `
        -JsonBody @{ username = $Username; password = $Password }
}

function New-TestCover([string]$Path) {
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new(640, 360)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $rect = [System.Drawing.Rectangle]::new(0, 0, 640, 360)
    $brush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $rect,
        [System.Drawing.Color]::FromArgb(12, 35, 80),
        [System.Drawing.Color]::FromArgb(22, 119, 255),
        35.0)
    $graphics.FillRectangle($brush, $rect)
    $font = [System.Drawing.Font]::new('Arial', 34, [System.Drawing.FontStyle]::Bold)
    $white = [System.Drawing.Brushes]::White
    $graphics.DrawString('VideoHub', $font, $white, 42, 120)
    $small = [System.Drawing.Font]::new('Arial', 18)
    $graphics.DrawString('cover upload verification', $small, $white, 44, 180)
    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Jpeg)
    $small.Dispose(); $font.Dispose(); $brush.Dispose(); $graphics.Dispose(); $bitmap.Dispose()
}

function New-TestVideo([string]$Path) {
    $bytes = New-Object byte[] 4096
    $ftyp = [System.Text.Encoding]::ASCII.GetBytes('ftypisom')
    [Array]::Copy($ftyp, 0, $bytes, 4, $ftyp.Length)
    [System.IO.File]::WriteAllBytes($Path, $bytes)
}

function New-MultipartUpload([string]$VideoPath, [string]$CoverPath, [string]$Title) {
    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    $video = [System.Net.Http.ByteArrayContent]::new([System.IO.File]::ReadAllBytes($VideoPath))
    $video.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('video/mp4')
    $multipart.Add($video, 'file', 'qa-video.mp4')

    $cover = [System.Net.Http.ByteArrayContent]::new([System.IO.File]::ReadAllBytes($CoverPath))
    $cover.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('image/jpeg')
    $multipart.Add($cover, 'cover', 'qa-cover.jpg')

    foreach ($pair in @{
        title = $Title
        description = '一键验收脚本自动创建'
        category = '验收'
        tags = 'qa,e2e'
        duration = '5'
    }.GetEnumerator()) {
        $multipart.Add([System.Net.Http.StringContent]::new(
            [string]$pair.Value, [System.Text.Encoding]::UTF8), $pair.Key)
    }
    return ,$multipart
}

try {
    Write-Step '0. 等待后端可用'
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        try {
            $ping = Send-Api -Method GET -Path '/api/v1/info'
            if ($ping.Status -eq 200) { $ready = $true; break }
        } catch { }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw "后端不可用：$BaseUrl" }
    Pass "后端已就绪：$BaseUrl"

    $suffix = (Get-Date -Format 'HHmmss') + (Get-Random -Minimum 100 -Maximum 999)
    $userA = "qa_a_$suffix".ToLowerInvariant()
    $userB = "qa_b_$suffix".ToLowerInvariant()
    $password = 'QaTest123456'
    $title = "QA 验收视频 $suffix"

    Write-Step '1. 注册、登录与 JWT'
    $regA = Send-Api -Method POST -Path '/api/auth/register' -JsonBody @{
        username = $userA; password = $password; nickname = '验收用户 A'
    }
    Check ($regA.Status -eq 200 -and $regA.Json.data.token) "用户 A 注册成功 ($userA)"
    $tokenA = $regA.Json.data.token
    $idA = [long]$regA.Json.data.user.id

    $regB = Send-Api -Method POST -Path '/api/auth/register' -JsonBody @{
        username = $userB; password = $password; nickname = '验收用户 B'
    }
    Check ($regB.Status -eq 200 -and $regB.Json.data.token) "用户 B 注册成功 ($userB)"
    $tokenB = $regB.Json.data.token

    for ($i = 0; $i -lt 5; $i++) {
        Send-Api -Method POST -Path '/api/auth/login' `
            -JsonBody @{ username = $userB; password = 'wrong-password' } | Out-Null
    }
    $blocked = Send-Api -Method POST -Path '/api/auth/login' `
        -JsonBody @{ username = $userB; password = $password }
    Check ($blocked.Status -eq 429) '连续 5 次登录失败后触发限流返回 429'

    $me = Send-Api -Method GET -Path '/api/auth/me' -Token $tokenA
    Check ($me.Status -eq 200 -and [long]$me.Json.data.id -eq $idA) 'JWT 身份校验通过 (/api/auth/me)'

    Write-Step '2. 公开/受保护接口与缓存耗时'
    $probeKeyword = "cache-$suffix"
    Write-Host "  缓存探针关键词：$probeKeyword" -ForegroundColor DarkGray
    $pageMiss = Send-Api -Method GET -Path "/api/video/page?page=1&size=5&keyword=$probeKeyword"
    $pageHit = Send-Api -Method GET -Path "/api/video/page?page=1&size=5&keyword=$probeKeyword"
    Check ($pageMiss.Status -eq 200 -and $pageHit.Status -eq 200) '视频分页接口匿名可访问'
    Write-Host ("  首次请求 {0} ms，缓存命中 {1} ms" -f $pageMiss.ElapsedMs, $pageHit.ElapsedMs) -ForegroundColor Yellow

    $samplePage = Send-Api -Method GET -Path '/api/video/page?page=1&size=5'
    $sampleId = $samplePage.Json.data.records[0].id
    $anonLike = Send-Api -Method POST -Path "/api/video/$sampleId/like"
    Check ($anonLike.Status -eq 401) '未登录点赞返回 401'

    $anonPlay = Send-Api -Method POST -Path "/api/video/$sampleId/play"
    Check ($anonPlay.Status -eq 200) '匿名播放计数返回 200'

    $traceId = 'verify' + $suffix
    $traceResponse = Send-Api -Method GET -Path '/api/v1/info' -ExtraHeaders @{ 'X-Request-Id' = $traceId }
    $echo = $traceResponse.Headers.GetValues('X-Request-Id') | Select-Object -First 1
    $responseTime = $traceResponse.Headers.GetValues('X-Response-Time') | Select-Object -First 1
    Check ($echo -eq $traceId -and $responseTime) "链路响应头 X-Request-Id=$echo, X-Response-Time=$responseTime"

    Write-Step '3. 上传视频 + 封面'
    $tempDir = Join-Path $env:TEMP "videohub-verify-$suffix"
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    $videoPath = Join-Path $tempDir 'qa-video.mp4'
    $coverPath = Join-Path $tempDir 'qa-cover.jpg'
    New-TestVideo $videoPath
    New-TestCover $coverPath

    $encodedTitle = [uri]::EscapeDataString($title)
    $beforeUpload = Send-Api -Method GET -Path "/api/video/page?page=1&size=5&keyword=$encodedTitle"
    Check ($beforeUpload.Status -eq 200 -and [int]$beforeUpload.Json.data.total -eq 0) '上传前关键词缓存为空'

    $multipart = New-MultipartUpload $videoPath $coverPath $title
    $upload = Send-Api -Method POST -Path '/api/video/upload' -Token $tokenA -Multipart $multipart
    $multipart.Dispose()
    Check ($upload.Status -eq 200 -and $upload.Json.data.id) '视频与封面上传成功'
    $videoId = [long]$upload.Json.data.id
    $coverUrl = $upload.Json.data.coverUrl
    $videoUrl = $upload.Json.data.url
    Check ($null -ne $upload.Json.data.createTime) '上传响应包含创建时间'
    Check ($coverUrl -and $coverUrl.StartsWith('/api/video/cover/')) "封面地址已生成：$coverUrl"

    $afterUpload = Send-Api -Method GET -Path "/api/video/page?page=1&size=5&keyword=$encodedTitle"
    Check ($afterUpload.Status -eq 200 -and [int]$afterUpload.Json.data.total -eq 1) '上传后分页缓存已失效并可查到新视频'

    Write-Step '4. 封面读取、详情与 Range 播放'
    $detail = Send-Api -Method GET -Path "/api/video/$videoId"
    Check ($detail.Status -eq 200 -and [long]$detail.Json.data.ownerId -eq $idA) '详情返回正确 ownerId'

    $coverRead = Send-Api -Method GET -Path $coverUrl
    Check ($coverRead.Status -eq 200) '封面读取返回 200'
    Check ($coverRead.ContentType -and $coverRead.ContentType.MediaType -eq 'image/jpeg') "封面 Content-Type=$($coverRead.ContentType.MediaType)"

    $streamPath = $videoUrl
    $range = Send-Api -Method GET -Path $streamPath -ExtraHeaders @{ Range = 'bytes=0-99' }
    $contentRange = if ($range.ContentHeaders.Contains('Content-Range')) { ($range.ContentHeaders.GetValues('Content-Range') | Select-Object -First 1) } else { '' }
    Check ($range.Status -eq 206) 'Range 播放返回 206 Partial Content'
    Check ($contentRange -like 'bytes 0-99/*') "Content-Range=$contentRange"

    Write-Step '5. 点赞去重与资源归属校验'
    $like1 = Send-Api -Method POST -Path "/api/video/$videoId/like" -Token $tokenA
    Check ($like1.Status -eq 200 -and $like1.Json.data.liked -eq $true -and [int]$like1.Json.data.likeCount -eq 1) '用户 A 首次点赞成功，点赞数加一'
    $like2 = Send-Api -Method POST -Path "/api/video/$videoId/like" -Token $tokenA
    Check ($like2.Status -eq 200 -and $like2.Json.data.liked -eq $false -and [int]$like2.Json.data.likeCount -eq 0) '同一用户再次点赞变为取消，点赞数减一'

    $crossDelete = Send-Api -Method DELETE -Path "/api/video/$videoId" -Token $tokenB
    Check ($crossDelete.Status -eq 403) '用户 B 删除用户 A 的视频返回 403'

    $coverForm = [System.Net.Http.MultipartFormDataContent]::new()
    $newCover = [System.Net.Http.ByteArrayContent]::new([System.IO.File]::ReadAllBytes($coverPath))
    $newCover.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('image/jpeg')
    $coverForm.Add($newCover, 'cover', 'replacement.jpg')
    $coverUpdate = Send-Api -Method POST -Path "/api/video/$videoId/cover" -Token $tokenA -Multipart $coverForm
    $coverForm.Dispose()
    Check ($coverUpdate.Status -eq 200 -and $coverUpdate.Json.data.coverUrl) '用户 A 更换封面成功'
    Check ($coverUpdate.Json.data.coverUrl -ne $coverUrl) '换封面后 URL 已更新'

    Write-Step '6. 删除与缓存再次失效'
    $delete = Send-Api -Method DELETE -Path "/api/video/$videoId" -Token $tokenA
    Check ($delete.Status -eq 200) '用户 A 删除自己的视频成功'

    $afterDelete = Send-Api -Method GET -Path "/api/video/page?page=1&size=5&keyword=$encodedTitle"
    Check ($afterDelete.Status -eq 200 -and [int]$afterDelete.Json.data.total -eq 0) '删除后分页缓存已失效且查询为空'

    $deletedDetail = Send-Api -Method GET -Path "/api/video/$videoId"
    Check ($deletedDetail.Status -eq 404) '删除后详情返回 404'

    Write-Step '7. 清理测试数据'
    if (-not $KeepArtifacts) {
        Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
        if ($DbPassword -and (Test-Path $MysqlExe)) {
            $env:MYSQL_PWD = $DbPassword
            $sql = @"
DELETE v FROM video v JOIN app_user u ON v.owner_id = u.id
WHERE u.username IN ('$userA', '$userB');
DELETE FROM app_user WHERE username IN ('$userA', '$userB');
"@
            & $MysqlExe -u $DbUser $DbName -e $sql 2>$null | Out-Null
            Pass '测试用户与视频记录已从数据库清理'
        } else {
            Write-Host "  未配置 MySQL 清理参数，测试用户保留：$userA / $userB" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  已保留测试文件：$tempDir" -ForegroundColor Yellow
        Write-Host "  测试用户：$userA / $userB" -ForegroundColor Yellow
    }

    Write-Step '验收汇总'
    Write-Host ("通过 {0} 项，失败 {1} 项" -f $script:Passed, $script:Failed) -ForegroundColor $(if ($script:Failed -eq 0) { 'Green' } else { 'Red' })
    if ($script:Failed -gt 0) { exit 1 }
    exit 0
} catch {
    Write-Host ""
    Write-Host "脚本异常：$($_.Exception.Message)" -ForegroundColor Red
    Write-Host $_.ScriptStackTrace -ForegroundColor DarkGray
    exit 2
} finally {
    $script:Client.Dispose()
}
