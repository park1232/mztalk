package com.mztalk.resource.controller;


import com.mztalk.resource.domain.dto.ImagesDto;
import com.mztalk.resource.domain.entity.Result;
import com.mztalk.resource.domain.response.ResponseData;
import com.mztalk.resource.service.DeleteImageService;
import com.mztalk.resource.service.InsertImageService;
import com.mztalk.resource.service.SelectImageService;
import com.mztalk.resource.service.UpdateImageService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
@Api(tags = "image-api")
public class ImageApiController {

    private final InsertImageService insertImageService;

    private final SelectImageService selectImageService;

    private final UpdateImageService updateImageService;

    private final DeleteImageService deleteImageService;



    // 이미지 단일업로드 or 서브 이미지 업로드 or 기존 사진에 서브 이미지 업로드
    @ApiOperation(value="이미지를 단일로 보낼 수 있게 합니다.")
    @PostMapping("/image")
    public ResponseEntity<?> insertImage(@RequestParam("image")MultipartFile multipartFile, ImagesDto imagesDto) throws IOException {
        return insertImageService.insertImage(multipartFile, imagesDto);
    }

    // 사진 다중업로드
    @ApiOperation(value="이미지를 다중으로 보낼 수 있게 합니다.")
    @PostMapping("/images")
    public ResponseEntity<?> insertImages(@RequestParam("image") List<MultipartFile> multipartFileList, ImagesDto imagesDto){
        return insertImageService.insertImages(multipartFileList, imagesDto);
    }

    // 메인 이미지 업로드
    @ApiOperation(value="메인사진 업로드", notes = "메인이미지로 설정하고자 하는 곳에 요청을 보내면 자동으로 메인사진으로 등록됩니다. 후에 정보 요청을 통해서 동기화를 진행해주세요.")
    @PostMapping("/main-image")
    public ResponseEntity<?> insertMainImage(@RequestParam("image")MultipartFile multipartFile, ImagesDto imagesDto){

        return insertImageService.insertMainImage(multipartFile, imagesDto);
    }


    // 해당 글의 모든 사진데이터 불러오기
    @ApiOperation(value="해당 글 이미지 정보 조회", notes = "해당 서비스의 글번호에 해당하는 모든 이미지를 리스트로 가져옵니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name="bNo", value="해당 글 번호",dataType="long", paramType = "param"),
            @ApiImplicitParam(name="serviceName", value="해당 서비스 이름",dataType="String",  paramType = "param")
    })
    @GetMapping(value="/images" , consumes = "text/html")
    public ResponseEntity<?> getImages(@RequestParam("bNo")long bNo, @RequestParam("serviceName")String serviceName){
        return selectImageService.getImageInfo(bNo, serviceName);
    }

    // 해당 글의 메인사진만 데이터 불러오기
    @GetMapping(value="/main-image", consumes = "text/html")
    public ResponseEntity<?> getMainImage(@RequestParam("bNo")long bNo, @RequestParam("serviceName")String serviceName){
        return selectImageService.getMainImage(bNo, serviceName);
    }

    // 해당 글의 서브사진만 데이터 불러오기
    @GetMapping(value="/sub-image",  consumes = "text/html")
    public ResponseEntity<?> getSubImages(@RequestParam("bNo")long bNo, @RequestParam("serviceName")String serviceName){
        return selectImageService.getSubImages(bNo, serviceName);
    }


//    // 수정페이지에서 메인사진 변경하기
//    @PostMapping(value="/main-image", consumes = "text/html")
//    public ResponseEntity<?> changeMainImage(@RequestParam("image")MultipartFile multipartFile, ImagesDto imagesDto){
//        return updateImageService.changeMainImage(multipartFile, imagesDto);
//    }

     // 수정페이지에서 메인사진 변경하기
     // 여기서 imageName은 메인으로 등록하고자 하는 파일의 이름.
    @PatchMapping(value="/main-image", consumes = "text/html")
    public ResponseEntity<?> changeMainImage(@RequestParam("bNo")long bNo, @RequestParam("serviceName")String serviceName, @RequestParam("imageName")String objectKey){
        return updateImageService.changeMainImage(bNo, serviceName, objectKey);
    }

    // 해당 글사진 삭제
    @DeleteMapping(value= "/images", consumes = "text/html")
    public ResponseEntity<?> deleteImage(@RequestParam("bNo")long bNo, @RequestParam("serviceName")String serviceName){
        return deleteImageService.deleteImage(bNo, serviceName);
    }

    // 단일 파일 삭제
    @DeleteMapping(value = "/image-detail", consumes = "text/html")
    public ResponseEntity<?> deleteImageDetail(@RequestParam("imageName")String objectKey){
        return deleteImageService.deleteImageDetail(objectKey);
    }
}

// 각 DB에 필요한