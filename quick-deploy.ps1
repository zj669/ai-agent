# AI Agent 一键部署到测试服务器
# Windows PowerShell 脚本
# 使用方法: .\quick-deploy.ps1

# 配置参数
$SERVER_IP = "81.69.37.254"
$SERVER_USER = "root"
$REMOTE_PATH = "/opt/ai-agent"
$LOCAL_PATH = "."

# 颜色输出函数
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Write-Info($message) {
    Write-ColorOutput Green "[INFO] $message"
}

function Write-Warn($message) {
    Write-ColorOutput Yellow "[WARN] $message"
}

function Write-Error-Custom($message) {
    Write-ColorOutput Red "[ERROR] $message"
}

function Write-Step($step, $total, $message) {
    Write-ColorOutput Cyan "[$step/$total] $message"
}

# 显示标题
Write-Host ""
Write-ColorOutput Blue "========================================="
Write-ColorOutput Blue "   AI Agent 快速部署到测试服务器"
Write-ColorOutput Blue "   服务器: $SERVER_IP"
Write-ColorOutput Blue "========================================="
Write-Host ""

# 检查 SSH 连接
Write-Step 0 4 "检查 SSH 连接..."
$sshTest = ssh -o ConnectTimeout=5 -o BatchMode=yes ${SERVER_USER}@${SERVER_IP} "echo 2>&1" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Warn "SSH 连接失败,请确保:"
    Write-Host "  1. 已安装 OpenSSH 客户端"
    Write-Host "  2. 可以访问服务器 $SERVER_IP"
    Write-Host "  3. 已配置 SSH 密钥或准备好密码"
    Write-Host ""
    Write-Host "继续部署需要输入密码..."
}
Write-Info "SSH 连接检查完成"
Write-Host ""

# 步骤 1: 上传代码
Write-Step 1 4 "上传代码到服务器..."
Write-Info "正在上传,请稍候..."

# 使用 SCP 上传(排除不必要的文件)
$excludeFiles = @(
    "node_modules",
    "target",
    ".git",
    "logs",
    "*.log",
    ".idea",
    ".vscode"
)

# 创建临时排除文件列表
$tempExcludeFile = [System.IO.Path]::GetTempFileName()
$excludeFiles | Out-File -FilePath $tempExcludeFile -Encoding ASCII

# 使用 SCP 上传
scp -r -o "ConnectTimeout=10" ${LOCAL_PATH}\* ${SERVER_USER}@${SERVER_IP}:${REMOTE_PATH}/

if ($LASTEXITCODE -eq 0) {
    Write-Info "代码上传成功"
} else {
    Write-Error-Custom "代码上传失败"
    Remove-Item $tempExcludeFile -ErrorAction SilentlyContinue
    exit 1
}

Remove-Item $tempExcludeFile -ErrorAction SilentlyContinue
Write-Host ""

# 步骤 2: 检查 Docker 环境
Write-Step 2 4 "检查服务器 Docker 环境..."
$dockerCheck = ssh ${SERVER_USER}@${SERVER_IP} "docker --version 2>&1"

if ($LASTEXITCODE -eq 0) {
    Write-Info "Docker 已安装: $dockerCheck"
} else {
    Write-Warn "Docker 未安装,正在安装..."
    ssh ${SERVER_USER}@${SERVER_IP} "curl -fsSL https://get.docker.com | sh && systemctl start docker && systemctl enable docker"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Docker 安装完成"
    } else {
        Write-Error-Custom "Docker 安装失败"
        exit 1
    }
}
Write-Host ""

# 步骤 3: 执行部署
Write-Step 3 4 "执行部署脚本..."
Write-Info "正在构建镜像并启动容器,这可能需要几分钟..."

ssh ${SERVER_USER}@${SERVER_IP} "cd ${REMOTE_PATH} && chmod +x deploy.sh && ./deploy.sh"

if ($LASTEXITCODE -eq 0) {
    Write-Info "部署脚本执行成功"
} else {
    Write-Error-Custom "部署失败,请查看日志"
    Write-Host ""
    Write-Host "查看日志命令:"
    Write-Host "  ssh ${SERVER_USER}@${SERVER_IP} 'docker logs ai-agent-backend'"
    exit 1
}
Write-Host ""

# 步骤 4: 验证部署
Write-Step 4 4 "验证部署..."
Write-Info "等待应用启动..."
Start-Sleep -Seconds 10

# 检查健康状态
$healthCheck = ssh ${SERVER_USER}@${SERVER_IP} "curl -s http://localhost:8080/actuator/health 2>&1"

if ($healthCheck -like "*UP*") {
    Write-Info "✓ 后端健康检查通过"
} else {
    Write-Warn "✗ 后端健康检查失败,请查看日志"
    Write-Host "  健康检查响应: $healthCheck"
}

# 检查容器状态
Write-Info "检查容器状态..."
$containerStatus = ssh ${SERVER_USER}@${SERVER_IP} "docker ps | grep ai-agent"
Write-Host $containerStatus

Write-Host ""
Write-ColorOutput Blue "========================================="
Write-ColorOutput Green "部署完成!"
Write-Host ""
Write-Host "访问地址:"
Write-ColorOutput Cyan "  前端: http://${SERVER_IP}"
Write-ColorOutput Cyan "  后端: http://${SERVER_IP}:8080"
Write-ColorOutput Cyan "  健康检查: http://${SERVER_IP}:8080/actuator/health"
Write-Host ""
Write-Host "查看日志:"
Write-ColorOutput Yellow "  ssh ${SERVER_USER}@${SERVER_IP} 'docker logs -f ai-agent-backend'"
Write-Host ""
Write-Host "管理容器:"
Write-Host "  查看状态: ssh ${SERVER_USER}@${SERVER_IP} 'docker ps'"
Write-Host "  重启后端: ssh ${SERVER_USER}@${SERVER_IP} 'docker restart ai-agent-backend'"
Write-Host "  重启前端: ssh ${SERVER_USER}@${SERVER_IP} 'docker restart ai-agent-frontend'"
Write-ColorOutput Blue "========================================="
Write-Host ""

# 询问是否在浏览器中打开
$openBrowser = Read-Host "是否在浏览器中打开应用? (Y/N)"
if ($openBrowser -eq "Y" -or $openBrowser -eq "y") {
    Start-Process "http://${SERVER_IP}"
    Start-Process "http://${SERVER_IP}:8080/actuator/health"
}
