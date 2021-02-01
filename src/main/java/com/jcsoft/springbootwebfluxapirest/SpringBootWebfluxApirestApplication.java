package com.jcsoft.springbootwebfluxapirest;

import com.jcsoft.springbootwebfluxapirest.model.document.Category;
import com.jcsoft.springbootwebfluxapirest.model.document.Product;
import com.jcsoft.springbootwebfluxapirest.model.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.LocalDate;

@SpringBootApplication
public class SpringBootWebfluxApirestApplication
        implements CommandLineRunner
{
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootWebfluxApirestApplication.class);
    @Autowired
    private ProductService productService;
    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    public static void main(String[] args)
    {
        ReactorDebugAgent.init();
        SpringApplication.run(SpringBootWebfluxApirestApplication.class, args);
    }

    @Override
    public void run(final String... args)
            throws Exception
    {
        reactiveMongoTemplate.dropCollection("product")
                             .subscribe();
        reactiveMongoTemplate.dropCollection("category")
                             .subscribe();

        final Category catElec = Category.builder()
                                         .name("Electrónico")
                                         .build();
        final Category catDep = Category.builder()
                                        .name("Deporte")
                                        .build();
        final Category catComp = Category.builder()
                                         .name("Computación")
                                         .build();
        final Category catMueb = Category.builder()
                                         .name("Muebles")
                                         .build();

        Flux.just(catElec, catDep, catComp, catMueb)
            .flatMap(productService::saveCat)
            .doOnNext(cat -> {
                LOG.info("Categoria creada: {}, Id: {}", cat.getName(), catElec.getId());
            }) //then Many se usa cuando se desea que un flujo continue luego de que termine otro (parecido al then pero para flux)
            .thenMany(Flux.just(Product.builder()
                                       .name("TV Samsung LED")
                                       .price(550.5)
                                       .category(catElec)
                                       .build(),
                                Product.builder()
                                       .name("Camara Sony 4K")
                                       .price(1220.5)
                                       .category(catElec)
                                       .build(),
                                Product.builder()
                                       .name("Apple iPod")
                                       .price(980.2)
                                       .category(catElec)
                                       .build(),
                                Product.builder()
                                       .name("Sony Notebook")
                                       .price(1125.5)
                                       .category(catComp)
                                       .build(),
                                Product.builder()
                                       .name("HP Multifuncional")
                                       .price(425.3)
                                       .category(catComp)
                                       .build(),
                                Product.builder()
                                       .name("Bianchi Bicicleta")
                                       .price(125.2)
                                       .category(catDep)
                                       .build(),
                                Product.builder()
                                       .name("Dell Latitude 7400")
                                       .price(1428.4)
                                       .category(catComp)
                                       .build(),
                                Product.builder()
                                       .name("TV Sony Bravia OLED")
                                       .price(1800.5)
                                       .category(catElec)
                                       .build(),
                                Product.builder()
                                       .name("Sofa Cama")
                                       .price(1200.4)
                                       .category(catMueb)
                                       .build())
                          //el save devuelve Mono y queremos el product, asi que el flatMap convierte los elementos de cada Mono en un solo Flux que contiene products
                          .flatMap(prod -> {
                              prod.setCreateAt(LocalDate.now());
                              return productService.save(prod);
                          }))
            .subscribe(prod -> LOG.info("Insert: {} - {}", prod.getId(), prod.getName()));
    }
}
