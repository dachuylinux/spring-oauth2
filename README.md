# spring-oauth2
Chương trình demo sử dụng kết nối tới cổng SSO
1. Bổ sung các trường thông tin trong file _application.properties_
2. Chạy 2 chương trình port 8080,8081 _SERVER_PORT=\<port\> mvn springboot:run_
3. Truy cập địa chỉ http://localhost:8080
--------------------------
**Cấu hình SSO platform Đăng ký Service Provider**
<br/>

1. Truy cập _Identity > Service Provider > Add_. Nhập thông tin tên dịch vụ rồi bấm _Register_ (Tên dịch vụ là duy nhất, không trùng tên với những dịch vụ đang hoạt động)
2. Ở màn hình thông tin hiện ra, chọn _Inbound Authentication Configuration > OAuth/OpenID Connect Configuration_, click _Configure_
3. Giữ nguyên các tùy chọn mặc định, nhập thông tin cho field _Callback Url_ (redirect_uri sau khi đăng nhập sso thành công sẽ trả về để nhận giá trị code).<br/>
Ví dụ: sử dụng nhiều redirect_uri <br/>
_regexp=(http://localhost:8080/callback|http://localhost:8081/callback)_ <br/>
hoặc giá trị đơn <br/> _http://localhost:8081/callback_ <br/>
4. Click _Add_<br/>
5. Sau khi config thành công, Hệ thống tự động tạo cặp key (client_id + secret_key). Cung cấp cặp key này cho phía client sử dụng
--------------------------
**Mô tả thông tin các trường trong file _application.properties_** <br/>
_lgsp.token.base64_: như trong tài liệu đặc tả kết nối <br/>
_lgsp.uri.endpoint_: như trong tài liệu đặc tả kết nối <br/>
_sso.uri.endpoint_: địa chỉ dịch vụ SSO <br/>
_sso.client.id_: giá trị client_id ở bước cấu hình đăng ký service provider phía trên <br/> 
sso.client.secret: giá trị secret_key ở bước cấu hình đăng ký service provider phía trên <br/>