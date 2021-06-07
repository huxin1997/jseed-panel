#scp  ./target/jseed-panel-0.0.1-SNAPSHOT.jar  root@lolyk.com:/root/wwwroot/jseed-panel   
echo '上传成功'
 ssh root@lolyk.com  "sh /root/wwwroot/jseed-panel/start.sh"

