package com.mztalk.main.domain.reply.controller;


import com.mztalk.main.common.CMRespDto;
import com.mztalk.main.domain.reply.Reply;
import com.mztalk.main.domain.reply.service.ReplyService;
import com.mztalk.main.domain.reply.dto.ReplyRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/story")
public class ReplyApiController {

    private final ReplyService replyService;

//    @PostMapping("/api/reply")
//    public ResponseEntity<?> saveReply(@Validated @RequestBody ReplyRequestDto replyRequestDto,  BindingResult bindingResult){
//
//        replyService.saveReply(replyRequestDto);
//        return new ResponseEntity<>(new CMRespDto<>(1, "댓글쓰기성공", ), HttpStatus.CREATED);
//    @PostMapping("/{id}")
//    public Long saveReply(@PathVariable("id") Long id, ReplyRequestDto replyRequestDto){
//
//        return replyService.replySave(id, replyRequestDto);
//    }


    //댓글쓰기
    @ResponseBody
    @PostMapping("/board/{id}/reply")
    public ResponseEntity<?> replySave(@PathVariable("id") Long id, @RequestBody ReplyRequestDto replyRequestDto){

       Reply reply = replyService.replySave(id, replyRequestDto);

        return new ResponseEntity<>(new CMRespDto<>(1, "댓글쓰기성공", reply), HttpStatus.CREATED);
//        return new ResponseEntity<>(new CMRespDto<>(1, "댓글쓰기성공", result), HttpStatus.CREATED);
    }

    //댓글삭제
    @DeleteMapping("/board/{id}/reply/{replyId}")
    public Long deleteScore(
            @PathVariable("id") Long id,
            @PathVariable("replyId") Long ReplyId
    ){
       return replyService.deleteReply(id, ReplyId);
    }



}
