package demo.imgupload.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.imgupload.bean.Img;
import demo.imgupload.mapper.ImgMapper;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Controller
public class ImgController{

    private final ImgMapper imgMapper;
    private ObjectMapper mapper = new ObjectMapper();

    public ImgController(ImgMapper imgMapper){
        this.imgMapper = imgMapper;
    }

    @RequestMapping("/")
    public String root(){
        return "/pages/root.html";
    }

    @RequestMapping("/root")
    public String toRoot(){
        return "/pages/root.html";
    }

    @RequestMapping("/uploadImg")
    @ResponseBody
    //文件上传
    public String uploadImg(@RequestParam("fileName") MultipartFile file, @RequestParam("owner")String owner, HttpServletRequest request) throws IOException {

        String result;//上传结果信息
        Map<String,Object> map=new HashMap<String, Object>();

        if (file.getSize() / 1000 > 100){
            result="图片大小不能超过100KB";
        }
        else{
            //判断上传文件格式
            String fileType = file.getContentType();
            if (fileType.equals("image/jpeg") || fileType.equals("image/png") || fileType.equals("image/jpeg")) {
                //获取文件名
                String fileName = file.getOriginalFilename();

                //获取文件后缀名
                int index = fileName.lastIndexOf(".");
                String suffixName;
                if(index > 0) {
                    suffixName = fileName.substring(fileName.lastIndexOf("."));
                }else{
                    suffixName = ".png";
                }

                //重新生成文件名
                fileName = UUID.randomUUID()+suffixName;

                // 获取服务器路径(springboot虚拟服务器文件不适用)
                // String realPath = request.getServletContext().getRealPath("img");//文件的上传路径

                //获取项目路径
                Resource resource = new ClassPathResource("");
                String projectPath = resource.getFile().getAbsolutePath()+ "\\static\\img";
                System.out.println(projectPath);

                if (upload(projectPath, file, fileName)) {
                    //文件存放的相对路径(一般存放在数据库用于img标签的src)
                    String relativePath="img/"+fileName;
                    int row = saveImg(owner, relativePath);
                    if(row > 0)
                        result = "图片上传成功";
                    else
                        result = "图片上传数据库失败";
                }
                else{
                    result="图片上传失败";
                }
            }
            else{
                result="图片格式不正确";
            }
        }

        //结果用json形式返回
        map.put("result",result);
        String resultJson;
        try {
            resultJson = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return e.toString();
        }

        System.out.println(resultJson);
        return resultJson;
    }

    @RequestMapping("/getImgPath")
    @ResponseBody
    //获取对应选手的图片路径，传递给前端
    public String getImgPathByOwner(@RequestParam("owner")String owner){
        List<Img> imgs= imgMapper.getImgByOwner(owner);
        HashMap<String, List> map = new HashMap<>();
        ArrayList<String> paths = new ArrayList<>();
        if(imgs!=null && !imgs.isEmpty()){
            for(Img i:imgs){
                paths.add(i.getPath());
            }
        }
        map.put("paths", paths);

        String result;
        try {
            result = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return e.toString();
        }

        return result;
    }

    private boolean upload(String realPath, MultipartFile file, String fileName){
        // 将img文件存入本地
        String path = realPath + "\\" + fileName;
        System.out.println(path);

        File dest = new File(path);

        //判断文件父目录是否存在

        if (!dest.getParentFile().exists()) {
            boolean b = dest.getParentFile().mkdir();
            if(!b){
                return b;
            }
        }

        //保存文件
        try {
            file.transferTo(dest);
            return true;
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private int saveImg(String owner, String path){

        //将图片信息存入数据库
        Img img = new Img();
        img.setOwner(owner);
        img.setPath(path);
        int row = imgMapper.insertImg(img);

        return row;
    }
}
