package com.example.pro_netpicture;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WebPictureActivity extends Activity {
    private Context mContext;
    private ImageView imageView;
    private Button button;

    //主线程，Activity中创建Handler对象
    private Handler mHandler=new Handler(){
        //主线程处理消息
        @Override
        public void handleMessage(@NonNull Message msg) {
            //获取图片
            Bitmap bitmap= (Bitmap) msg.obj;
            imageView.setImageBitmap(bitmap);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t_picture);
        //设置上下文
        mContext=WebPictureActivity.this;
        //获取ImageView
        imageView = findViewById(R.id.iv1);
        //获取按钮
        button = findViewById(R.id.btn_show);
        //设置按钮点击响应
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建进程
                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        //加载网络图片
                        loadWebPicture();
                    }
                };
                //执行进程
                thread.start();
            }
        });
        //设置信任https请求
        handleSSLHandshake();
    }
    //获取网络图片
    private void loadWebPicture() {
//        //是否有缓存逻辑
//        //判断是否已经有图片缓存
//        String urlString = "https://attach.bbs.miui.com/forum/201310/20/134248dczksturrfko9fuf.jpg";
//        String fileName = urlString.substring(urlString.lastIndexOf("/"+1));
//        File file = new File(getFilesDir(),fileName);
//        //缓存图片存在
//        if (file.exists()&&file.length()>0){
//            //直接加载缓存图片,通过文件路径获取位图
//            final Bitmap bm=BitmapFactory.decodeFile(file.getAbsolutePath());
//            //设置图片显示
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    imageView.setImageBitmap(bm);
//                }
//            });
//            return;
//        }

        //网络请求
        try{
            //创建URL
            //http://pics.sc.chinaz.com/files/pic/pic9/201912/zzpic21911.jpg
            //https://attach.bbs.miui.com/forum/201310/20/134248dczksturrfko9fuf.jpg
            URL url = new URL("https://attach.bbs.miui.com/forum/201310/20/134248dczksturrfko9fuf.jpg");
            //打开链接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            //设置请求类型
            connection.setRequestMethod("GET");
            //设置连接超时
            connection.setConnectTimeout(5000);
            int retCode = connection.getResponseCode();
            if (retCode == 200) {
                //获取输入流
                InputStream is = connection.getInputStream();
                //创建位图
                final Bitmap bitmap = BitmapFactory.decodeStream(is);
                //缓存文件
//                saveImageCache(url, bitmap);

//                //子线程更新UI
//                //方式1：
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        imageView.setImageBitmap(bitmap);
//                    }
//                });

                //方式2：发送信息，在handler中更新UI
                Message message=new Message();
                //使用message携带信息
                //message.arg1,arg2：int类型
                //message.obj:任意对象
                message.obj=bitmap;
                mHandler.sendMessage(message);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //保存图片缓存
    private void saveImageCache(URL url, Bitmap bitmap) throws FileNotFoundException {
        //将bitmap缓存，file文件夹下
        //获取文件名
        String urlPath = url.getPath();
        String fileName = urlPath.substring(urlPath.lastIndexOf("/")+1);
        //创建outputStream
        OutputStream outputStream=openFileOutput(fileName,MODE_PRIVATE);
        //format参数：文件类型；quality参数：质量0-100；
        bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
    }

    //报错：javax.net.ssl.SSLHandshakeException: java.security.cert.CertPathValidatorException: Trust anchor for certification path not found
    //Https请求的验证证书不支持,所有证书验证通过
    public static void handleSSLHandshake(){
        TrustManager[] trustManagers=new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };
        try {
            SSLContext sslContext=SSLContext.getInstance("TLS");
            //信任所有证书
            sslContext.init(null,trustManagers,new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    //任何hostname都验证通过
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }
    //报错：Cleartext HTTP traffic to img.pconline.com.cn not permitted，API29以后禁止提交http请求；
    //可以配置允许http请求
    //在AndroidManifest.xml中application配置 android:networkSecurityConfig="@xml/network_security_config"

}
