package com.jcsoft.springbootwebfluxapirest.controller;

import com.jcsoft.springbootwebfluxapirest.SpringBootWebfluxApirestApplication;
import com.jcsoft.springbootwebfluxapirest.model.document.Product;
import com.jcsoft.springbootwebfluxapirest.model.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/product")
public class ProductController
{
    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

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
    public Mono<ResponseEntity<Product>> create(@RequestBody Product product)
    {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDate.now());
        }

        return productService.save(product)
                             .map(prod -> ResponseEntity.created(URI.create("/api/product/".concat(prod.getId())))
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(prod) //El producto creado en el body.
                             );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> edit(@RequestBody Product product,
                                              @PathVariable String id)
    {
        LOG.info("Editing id= {}", id);
        return productService.findById(id)
                             .flatMap(prod -> {
                                 prod.setName(product.getName());
                                 prod.setPrice(product.getPrice());
                                 prod.setCategory(product.getCategory());
                                 return productService.save(prod);
                             })
                             .map(prod -> ResponseEntity.created(URI.create("/api/product/".concat(prod.getId())))
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(prod))
                             .defaultIfEmpty(ResponseEntity.notFound()
                                                           .build());
    }
}
