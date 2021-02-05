package com.jcsoft.springbootwebfluxapirest.controller;

import com.jcsoft.springbootwebfluxapirest.model.document.Product;
import com.jcsoft.springbootwebfluxapirest.model.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
public class ProductController
{
    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @Value("${config.uploads.path}")
    private String path;

    public ProductController(final ProductService productService)
    {
        this.productService = productService;
    }

    @GetMapping
    public Flux<Product> list()
    {
        return productService.findAll();
    }

    /**
     * @return Forma más manual y configurable.
     */
    @GetMapping("/lV2")
    public Mono<ResponseEntity<Flux<Product>>> listV2()
    {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(productService.findAll())
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> show(@PathVariable String id)
    {
        return productService.findById(id)
                .map(prod -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(prod))
                .defaultIfEmpty(ResponseEntity.notFound()
                        .build());
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> create(@Valid @RequestBody Mono<Product> productMono)
    {
        Map<String, Object> response = new HashMap<>();

        return productMono.flatMap(product -> {
            if (product.getCreateAt() == null) {
                product.setCreateAt(LocalDate.now());
            }
            return productService.save(product)
                    .map(prod -> {
                                response.put("product", prod);
                                response.put("success", "Producto creado con exito");
                                return ResponseEntity.created(URI.create("/api/product/".concat(prod.getId())))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(response); //El producto creado en el body.
                            }
                    );
        })
                // Manejo de errores
                .onErrorResume(t -> Mono.just(t)
                        // lo convierte a un tipo mas leible
                        .cast(WebExchangeBindException.class)
                        .flatMap(e -> Mono.just(e.getFieldErrors()))
                        // a partir de la lista dentro del Mono obtenemos un Flux de los elementos de la lista
                        .flatMapMany(Flux::fromIterable)
                        // Convertimos los elementos FieldError del Flux en Strings
                        .map(fieldError -> "El campo " + fieldError.getField() + " " +
                                           fieldError.getDefaultMessage())
                        .collectList()
                        .flatMap(list -> {
                            response.put("errors", list);
                            response.put("status", HttpStatus.BAD_REQUEST.value());
                            return Mono.just(ResponseEntity.badRequest()
                                    .body(response));
                        })
                );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> edit(@RequestBody Product product,
                                              @PathVariable String id)
    {
        LOG.info("Editing id= {}", id);
        return productService.findById(id)
                //el flat map toma el valor dentro del Mono, le hace lo que quiere y se debe devolver otro Mono con un valor del mismo o de otro tipo
                .flatMap(prod -> {
                    prod.setName(product.getName());
                    prod.setPrice(product.getPrice());
                    prod.setCategory(product.getCategory());
                    return productService.save(prod);
                })
                // el map toma el valor dentro del Mono, le hacemos lo que queremos y no debe devolver un Mono, sino el tipo que queremos que vaya en el Mono
                .map(prod -> ResponseEntity.created(URI.create("/api/product/".concat(prod.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(prod))
                .defaultIfEmpty(ResponseEntity.notFound()
                        .build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable String id)
    {
        return productService.findById(id)
                .flatMap(prod -> productService.delete(prod)
                        .then(Mono.just(ResponseEntity.noContent()
                                .build())))
                .defaultIfEmpty(ResponseEntity.notFound()
                        .build());
    }

    @PostMapping("/upload/{id}")
    public Mono<ResponseEntity<Product>> upload(@PathVariable String id,
                                                @RequestPart FilePart filePart)
    {
        return productService.findById(id)
                .flatMap(product -> {
                    product.setPhoto(UUID.randomUUID()
                            .toString()
                            .concat("-")
                            .concat(filePart.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", "")));
                    return filePart.transferTo(new File(path + product.getPhoto()))
                            .then(productService.save(product));
                })
                .map(product -> ResponseEntity.ok(product))
                .defaultIfEmpty(ResponseEntity.notFound()
                        .build());
    }

    @PostMapping("/v2")
    public Mono<ResponseEntity<Product>> createWithPhoto(Product product,
                                                         @RequestPart FilePart filePart)
    {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDate.now());
        }

        product.setPhoto(UUID.randomUUID()
                .toString()
                .concat("-")
                .concat(filePart.filename()
                        .replace(" ", "")
                        .replace(":", "")
                        .replace("\\", "")));

        return filePart.transferTo(new File(path + product.getPhoto()))
                .then(productService.save(product))
                .map(prod -> ResponseEntity.created(URI.create("/api/product/".concat(prod.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(prod) //El producto creado en el body.
                );
    }
}
