$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'your-database-password'
$env:DB_URL = 'jdbc:mysql://localhost:3306/kob?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8'
$env:ADMIN_PASSWORD = 'change-this-admin-password'
$env:JWT_SECRET = 'change-this-jwt-secret-to-a-long-random-string'

$env:MAIL_HOST = 'smtp.qq.com'
$env:MAIL_PORT = '465'
$env:MAIL_USERNAME = 'your-mail@example.com'
$env:MAIL_PASSWORD = 'your-smtp-authorization-code'
#该文件已废弃不建议使用，仅做兼容性保留（确信