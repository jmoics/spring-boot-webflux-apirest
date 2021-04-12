package com.jcsoft.springbootwebfluxapirest;

import com.jcsoft.springbootwebfluxapirest.handler.ProductHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

@Configuration
public class RouterFunctionConfiguration
{

    /**
     * El primer argumento de route es un predicado de solicitud. Observe cómo usamos un método RequestPredicates.GET importado estáticamente aquí.
     * El segundo parámetro define una función de controlador que se utilizará si se aplica el predicado
     *
     * @param handler HandlerFunction representa una función que genera respuestas para las solicitudes que se les envían
     * @return RouterFunction sirve como alternativa a la anotación @RequestMapping.
     * Podemos usarlo para enrutar solicitudes a las funciones del controlador
     */
    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler)
    {
        return RouterFunctions.route(GET("/api/v2/product").or(GET("/api/v3/product")), handler::list)
                .andRoute(GET("/api/v2/product/{id}").and(contentType(MediaType.APPLICATION_JSON)), handler::show)
                .andRoute(POST("/api/v2/product"), handler::create)
                .andRoute(PUT("/api/v2/product/{id}"), handler::edit)
                .andRoute(DELETE("/api/v2/product/{id}"), handler::delete)
                .andRoute(POST("/api/v2/product/upload/{id}"), handler::upload)
                .andRoute(POST("/api/v2/product/photo"), handler::createWithPhoto);
    }
}
