package com.jcsoft.springbootwebfluxapirest.model.service;

import com.jcsoft.springbootwebfluxapirest.model.document.Category;
import com.jcsoft.springbootwebfluxapirest.model.document.Product;
import com.jcsoft.springbootwebfluxapirest.model.repository.CategoryRepository;
import com.jcsoft.springbootwebfluxapirest.model.repository.ProductRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductServiceImpl
        implements ProductService
{
    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;

    public ProductServiceImpl(final ProductRepository productRepository,
                              final CategoryRepository categoryRepository)
    {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Flux<Product> findAll()
    {
        return productRepository.findAll();
    }

    @Override
    public Flux<Product> findAllWithNameUpperCase()
    {
        return productRepository.findAll()
                                .map(prod -> {
                                    prod.setName(prod.getName()
                                                     .toUpperCase());
                                    return prod;
                                });
    }

    @Override
    public Flux<Product> findAllWithNameUpperCaseRepeat()
    {
        return findAllWithNameUpperCase().repeat(5000);
    }

    @Override
    public Mono<Product> findById(final String id)
    {
        return productRepository.findById(id);
    }

    @Override
    public Mono<Product> save(final Product product)
    {
        return productRepository.save(product);
    }

    @Override
    public Mono<Void> delete(final Product product)
    {
        return productRepository.delete(product);
    }

    @Override
    public Flux<Category> findCatAll()
    {
        return categoryRepository.findAll();
    }

    @Override
    public Mono<Category> findCatById(final String id)
    {
        return categoryRepository.findById(id);
    }

    @Override
    public Mono<Category> saveCat(final Category category)
    {
        return categoryRepository.save(category);
    }
}
