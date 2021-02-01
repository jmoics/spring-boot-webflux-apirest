package com.jcsoft.springbootwebfluxapirest.model.repository;

import com.jcsoft.springbootwebfluxapirest.model.document.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ProductRepository
        extends ReactiveMongoRepository<Product, String>
{
}
