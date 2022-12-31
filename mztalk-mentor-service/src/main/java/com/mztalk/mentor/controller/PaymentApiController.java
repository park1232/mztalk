package com.mztalk.mentor.controller;

import com.mztalk.mentor.domain.dto.PaymentDto;
import com.mztalk.mentor.domain.dto.PaymentReqDto;
import com.mztalk.mentor.domain.entity.Result;
import com.mztalk.mentor.service.PaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 201, message = "CREATED"),
        @ApiResponse(code = 400, message = "BAD REQUEST"),
        @ApiResponse(code = 500, message = "SERVER ERROR")
})
@Api(tags = {"서비스 이용자가 결제한 정보를 제공하는 Controller"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/mentors")
public class PaymentApiController {
    private final PaymentService paymentService;

    @ApiOperation(value = "결제 정보 저장", notes = "결제 정보를 저장하는 메소드입니다.", response = Long.class)
    @PostMapping("/payment")
    public ResponseEntity<?> savePayment(@RequestBody PaymentReqDto paymentReqDto){
        Long savedId = paymentService.save(paymentReqDto);
        return new ResponseEntity<>(new Result<>("해당 결제가 정상적으로 저장되었습니다.",savedId), HttpStatus.OK);
    }

    @ApiOperation(value = "결제 정보 리턴", notes = "해당 번호에 해당하는 결제 정보를 리턴하는 메소드입니다.", response = Result.class)
    @GetMapping("/payment/{id}")
    public ResponseEntity<?> findPayment(@PathVariable("id")Long id){
        PaymentDto payment = paymentService.findById(id);
        return new ResponseEntity<>(new Result<>("해당 번호에 대한 결제 정보", payment), HttpStatus.OK);
    }

    @ApiOperation(value = "모든 결제 정보 리턴", notes = "모든 결제 정보를 리턴하는 메소드입니다.", response = Result.class)
    @GetMapping("/payments")
    public ResponseEntity<?> findAll(){
        List<PaymentDto> payments = paymentService.findAll();
        return new ResponseEntity<>(new Result<>("멘토 서비스에서 결제된 모든 결제 정보", payments), HttpStatus.OK);
    }

    @ApiOperation(value = "결제 취소", notes = "해당 번호에 해당하는 결제를 취소하는 메소드입니다.", response = Long.class)
    @PatchMapping("/payment/{id}")
    public Long cancelPayment(@PathVariable("id")Long id){
        return paymentService.cancel(id);
    }
}
