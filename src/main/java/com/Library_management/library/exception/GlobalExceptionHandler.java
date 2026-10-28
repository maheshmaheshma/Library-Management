package com.mahesh.library.exception;
import com.mahesh.library.dto.ApiDtos.ErrorResponse; import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*; import org.springframework.dao.DataIntegrityViolationException; import java.time.LocalDate;
@RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(NotFoundException.class) ResponseEntity<ErrorResponse> notFound(NotFoundException e){return response(404,"Not Found",e.getMessage());}
 @ExceptionHandler(ConflictException.class) ResponseEntity<ErrorResponse> conflict(ConflictException e){return response(409,"Conflict",e.getMessage());}
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class}) ResponseEntity<ErrorResponse> badRequest(RuntimeException e){return response(400,"Bad Request",e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e){String msg=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("Invalid request");return response(400,"Validation Failed",msg);}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ErrorResponse> dataConflict(DataIntegrityViolationException e){return response(409,"Conflict","A unique or database constraint was violated");}
 private ResponseEntity<ErrorResponse> response(int s,String error,String msg){return ResponseEntity.status(s).body(new ErrorResponse(s,error,msg,LocalDate.now()));}
}