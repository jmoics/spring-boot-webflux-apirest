package com.jcsoft.springbootwebfluxapirest.model.repository;

import com.jcsoft.springbootwebfluxapirest.model.document.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CategoryRepository
        extends ReactiveMongoRepository<Category, String>
{
}
