package com.jcsoft.springbootwebfluxapirest.handler;

import com.jcsoft.springbootwebfluxapirest.model.document.Category;
import com.jcsoft.springbootwebfluxapirest.model.document.Product;
import com.jcsoft.springbootwebfluxapirest.model.service.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Podría considerarse como el controlador.
 */
@Component
public class ProductHandler
{
    private final ProductService productService;
    private Validator validator;
    @Value("${config.uploads.path}")
    private String path;

    public ProductHandler(final ProductService productService,
                          @Qualifier("webFluxValidator") final Validator validator)
    {
        this.productService = productService;
        this.validator = validator;
    }

    /**
     * @param request Se utiliza ServerRequest como entrada.
     * @return Se utiliza ServerResponse como salida.
     */
    public Mono<ServerResponse> list(ServerRequest request)
    {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(productService.findAll(), Product.class);
    }

    public Mono<ServerResponse> show(ServerRequest request)
    {
        String id = request.pathVariable("id");
        return productService.findById(id)
                .flatMap(product -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        //.body(BodyInserters.fromValue(product))
                        // Alternativa a usar lo anterior
                        .bodyValue(product))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> create(ServerRequest request)
    {
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono.flatMap(product -> {
            Errors errors = new BeanPropertyBindingResult(product, Product.class.getName());
            validator.validate(product, errors);
            if (errors.hasErrors()) {
                return Flux.fromIterable(errors.getFieldErrors())
                        .map(fieldError -> String.format("El campo %s %s", fieldError.getField(), fieldError.getDefaultMessage()))
                        .collectList()
                        .flatMap(list -> ServerResponse.badRequest().bodyValue(list));
            } else {
                if (product.getCreateAt() == null) {
                    product.setCreateAt(LocalDate.now());
                }
                return productService.save(product).flatMap(productDB -> ServerResponse
                        .created(URI.create("/api/v2/product".concat(productDB.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(productDB)));
            }
        });
    }

    public Mono<ServerResponse> edit(ServerRequest request)
    {
        Mono<Product> productMono = request.bodyToMono(Product.class);
        String id = request.pathVariable("id");

        Mono<Product> productDb = productService.findById(id);

        return productDb.zipWith(productMono, (db, req) -> {
            db.setName(req.getName());
            db.setPrice(req.getPrice());
            db.setCategory(req.getCategory());
            return db;
        }).flatMap(product -> ServerResponse.created(URI.create("/api/v2/product/".concat(product.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(productService.save(product), Product.class)
        ).switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> delete(ServerRequest request)
    {
        String id = request.pathVariable("id");

        Mono<Product> productDb = productService.findById(id);

        return productDb.flatMap(product -> productService.delete(product).then(ServerResponse.noContent().build()))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> upload(ServerRequest request)
    {
        String id = request.pathVariable("id");
        return request.multipartData()
                .map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(filePart -> productService.findById(id).flatMap(product -> {
                    product.setPhoto(UUID.randomUUID().toString().concat("-").concat(filePart.filename())
                            .replace(" ", "").replace(":", "").replace("\\", ""));
                    return filePart.transferTo(new File(path.concat(product.getPhoto()))).then(
                            productService.save(product));
                })).flatMap(product -> ServerResponse.created(URI.create("/api/v2/product".concat(product.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(product)
                ).switchIfEmpty(ServerResponse.notFound().build());

    }

    public Mono<ServerResponse> createWithPhoto(ServerRequest request)
    {
        Mono<Product> monoProd = request.multipartData().map(multipart -> {
            FormFieldPart name = (FormFieldPart) multipart.toSingleValueMap().get("name");
            FormFieldPart price = (FormFieldPart) multipart.toSingleValueMap().get("price");
            FormFieldPart categoryId = (FormFieldPart) multipart.toSingleValueMap().get("category.id");
            FormFieldPart categoryName = (FormFieldPart) multipart.toSingleValueMap().get("category.name");

            final Category category = Category.builder().id(categoryId.value()).name(categoryName.value()).build();
            return Product.builder().name(name.value()).price(Double.parseDouble(price.value())).category(category).build();
        });
        return request.multipartData()
                .map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(filePart -> monoProd.flatMap(product -> {
                    product.setPhoto(UUID.randomUUID().toString().concat("-").concat(filePart.filename())
                            .replace(" ", "").replace(":", "")
                            .replace("\\", ""));
                    return filePart.transferTo(new File(path.concat(product.getPhoto()))).then(
                            productService.save(product));
                })).flatMap(product -> ServerResponse.created(URI.create("/api/v2/product".concat(product.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(product));

    }
}
