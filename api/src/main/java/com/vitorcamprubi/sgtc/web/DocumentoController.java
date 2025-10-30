package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.security.AuthService;
import com.vitorcamprubi.sgtc.service.DocumentoService;
import com.vitorcamprubi.sgtc.web.dto.DocumentoDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DocumentoController {
    private final DocumentoService service; private final AuthService auth;
    public DocumentoController(DocumentoService s, AuthService a){ this.service=s; this.auth=a; }

    @PostMapping(value="/grupos/{grupoId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentoDTO upload(@PathVariable Long grupoId,
                               @RequestPart("file") MultipartFile file,
                               @RequestPart("titulo") String titulo) throws IOException {
        var doc = service.upload(grupoId, titulo, file, auth.getCurrentUser());
        return DocumentoDTO.of(doc);
    }

    @GetMapping("/grupos/{grupoId}/documentos")
    public List<DocumentoDTO> listar(@PathVariable Long grupoId) {
        return service.listar(grupoId, auth.getCurrentUser()).stream().map(DocumentoDTO::of).toList();
    }

    @GetMapping("/documentos/{docId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long docId) throws IOException {
        return service.download(docId, auth.getCurrentUser());
    }
}
