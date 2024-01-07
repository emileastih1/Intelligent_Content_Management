package com.ea.architecture.domain.driven.presentation.document.api;

import com.ea.architecture.domain.driven.presentation.BaseRestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Document", description = "API gateway to query the document domain")
@RestController
public class DocumentQueryRestController extends BaseRestController {

}
