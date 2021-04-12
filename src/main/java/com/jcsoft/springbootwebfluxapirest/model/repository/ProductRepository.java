package com.jcsoft.springbootwebfluxapirest.model.repository;

import com.jcsoft.springbootwebfluxapirest.model.document.Product;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface ProductRepository
        extends ReactiveMongoRepository<Product, String>
{
    /**
     * Internamente se construye el query al identificar el nombre del metodo
     * @param name
     * @return
     */
    Mono<Product> findByName(String name);

    @Query("{ 'name': ?0 }")
    Mono<Product> get4Name(String name);
}
