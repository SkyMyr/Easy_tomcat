package com.tomcat.core;

import com.tomcat.annotations.Servlet;
import com.tomcat.baseservlet.AbstractServlet;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 监听请求,调用request和response对请求作出反应
 * @Author: myr
 * @Date: 2019/9/16 17:21
 * @version: 4.1
 */
public class HttpServer {

    /**
     * 监听端口
     */
    public static int port = 8088;

    /**
     * 关闭服务器的请求URI
     */
    static final String CLOSE_URI = "/shutdown";

    /**
     * Key值为Servlet的别名(uri),value为该Servlet对象
     * default权限
     */
    static HashMap<String, AbstractServlet> map;

    static {
        //包名,可以通过application.properties设置
        getServlets("com.tomcat.servlet");
    }

    /**
     * 单例,因为是通过主函数启动,不涉及多进程启动的问题,所以不需要做多线程方面的考虑
     */
    static ServerSocket serverSocket = null;

    /**
     * @Description : 多线程bio监听数据请求
     * @author : myr
     * @date : 2019/9/17 10:29
     */
    public void acceptWait() {
        try {
            //把连接请求队列的长度设为 3。这意味着当队列中有了 3 个连接请求时，如果 Client 再请求连接，就会被 Server拒绝，因为服务器队列已经满了
            //serverSocket = new ServerSocket(port,3);
            serverSocket = new ServerSocket(port);//默认50
        } catch (IOException e) {
            e.printStackTrace();
        }
        //扫描包,读取包下的所有Servlet

        while (!serverSocket.isClosed()) {
            try {
                //单线程,阻塞式监听(bio模式)
                Socket socket = serverSocket.accept();
                //version1 单线程  数据处理是主线程自己处理的，处理完这个任务才能处理下一个
                //version2 多线程 阻塞式BIO   主线程负责监听， 监听到之后分发给子线程去处理数据，响应
                RequestHandler handler = new RequestHandler(socket);
                handler.start();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    /**
     * @param packageName 包名,如com.tomcat.servlet
     * @return : void
     * @Description : 扫描packageName包下的所有带有@Servlet注解的类文件
     * @author : myr
     * @date : 2019/9/18 10:36
     */
    private static void getServlets(String packageName) {

        //class类的集合
        Set<Class<?>> classes = new LinkedHashSet<>();//set不重

        try {
            String packageDirName = packageName.replace(".", "/");//路径转换
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packageDirName);

            while (resources.hasMoreElements()) {//why?
                URL url = resources.nextElement();
                //System.out.println(url.getContent());
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClassesInPackageByFile(packageName, filePath,true,classes);
                }else if("jar".equals(protocol)){
                    //扫描JAR包
                }
            }
            //遍历class集合
            if (map == null){
                map = new HashMap<>(classes.size());
            }
            for (Class<?> aClass : classes) {
                //如果该class有Servlet注解
                if (aClass.isAnnotationPresent(Servlet.class)){
                    try {
                        //添加至map集合中
                        map.put(aClass.getAnnotation(Servlet.class).value(), (AbstractServlet) aClass.newInstance());//类加载的时候就已经定了，动态value值不能解决这个bug
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description : 对于file类型获取该类型的所有class   扫描包
     *
     * @param packageName 包名,com.tomcat.servlet
     * @param packagePath 包路径,com/tomcat/servlet
     * @param recursive 是否循环遍历子包内的文件
     * @param classes class集合
     * @return : void
     * @author : myr
     * @date : 2019/9/18 16:55
    */
    public static void findAndAddClassesInPackageByFile(String packageName,
                                                        String packagePath, final boolean recursive, Set<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "."
                                + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0,
                        file.getName().length() - 6);
                try {
                    // 添加到集合中去
                    // classes.add(Class.forName(packageName + '.' +
                    // className));
                    // 经过回复同学的提醒，这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                    classes.add(Thread.currentThread().getContextClassLoader()
                            .loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    // log.error("添加用户自定义视图类错误 找不到此类的.class文件");
                    e.printStackTrace();
                }
            }
        }
    }
}
