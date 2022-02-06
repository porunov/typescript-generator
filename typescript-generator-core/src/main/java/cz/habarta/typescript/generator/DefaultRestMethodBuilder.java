package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.MethodParameterModel;
import cz.habarta.typescript.generator.parser.RestMethodModel;
import cz.habarta.typescript.generator.parser.RestParam;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

public class DefaultRestMethodBuilder implements RestMethodBuilder{

    @Override
    public RestMethodModel build(Class<?> originClass, String name, Type returnType, Method originalMethod,
                                 Class<?> rootResource, String httpMethod, String path,
                                 List<MethodParameterModel> pathParams, List<RestParam> queryParams,
                                 MethodParameterModel entityParam, List<String> comments, List<RestParam> headers) {

        return new RestMethodModel(originClass, name, returnType, originalMethod, rootResource, httpMethod, path,
                pathParams, queryParams, entityParam, comments, headers);
    }

}
