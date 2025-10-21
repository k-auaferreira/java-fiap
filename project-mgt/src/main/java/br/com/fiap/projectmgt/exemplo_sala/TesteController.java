package br.com.fiap.projectmgt.exemplo_sala;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teste")
public class TesteController {

    @PostMapping
    public ResponseEntity<TesteDto> retornaDto
            (@Validated @RequestBody TesteDto dto){
        return ResponseEntity.ok(dto);
    }
}
