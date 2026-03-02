"""测试 SMTP 连接（不走代理）"""
import smtplib
import ssl
import socket

HOST = "smtp.qq.com"
PORT = 465
USERNAME = "3218356902@qq.com"
PASSWORD = "kgntyjxqfjtvdede"
TO = "testuser2@zjtest1231.us.ci"

# 1. 先测试 DNS 解析
print(f"[1] DNS 解析 {HOST} ...")
try:
    ips = socket.getaddrinfo(HOST, PORT)
    print(f"    解析成功: {ips[0][4]}")
except Exception as e:
    print(f"    DNS 解析失败: {e}")

# 2. 测试 TCP 连接
print(f"\n[2] TCP 连接 {HOST}:{PORT} ...")
try:
    sock = socket.create_connection((HOST, PORT), timeout=10)
    print(f"    TCP 连接成功")
    sock.close()
except Exception as e:
    print(f"    TCP 连接失败: {e}")

# 3. 测试 SMTP SSL 连接
print(f"\n[3] SMTP SSL 连接 ...")
try:
    ctx = ssl.create_default_context()
    with smtplib.SMTP_SSL(HOST, PORT, context=ctx, timeout=15) as server:
        print(f"    SSL 连接成功")
        
        # 4. 登录
        print(f"\n[4] SMTP 登录 {USERNAME} ...")
        server.login(USERNAME, PASSWORD)
        print(f"    登录成功")
        
        # 5. 发送测试邮件
        print(f"\n[5] 发送测试邮件到 {TO} ...")
        subject = "AI Agent 平台 - SMTP 测试"
        body = "这是一封测试邮件，验证 SMTP 连接是否正常。"
        msg = f"From: {USERNAME}\r\nTo: {TO}\r\nSubject: {subject}\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n{body}"
        server.sendmail(USERNAME, TO, msg.encode("utf-8"))
        print(f"    发送成功!")
        
except smtplib.SMTPAuthenticationError as e:
    print(f"    认证失败: {e}")
except smtplib.SMTPException as e:
    print(f"    SMTP 错误: {e}")
except ssl.SSLError as e:
    print(f"    SSL 错误: {e}")
except socket.timeout:
    print(f"    连接超时")
except Exception as e:
    print(f"    错误: {type(e).__name__}: {e}")

print("\n测试完成。")
