
package cz.habarta.typescript.generator.spring;

import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.RestMethodBuilder;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptFileType;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.parser.MethodParameterModel;
import cz.habarta.typescript.generator.parser.RestMethodModel;
import cz.habarta.typescript.generator.parser.RestParam;
import cz.habarta.typescript.generator.util.Utils;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

public class SpringTest {

    @Test
    public void testAnnotationUtils() {
        final Method greetingMethod = getMethod(SpringTestApplication.GreetingController.class, "greeting");
        final RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(greetingMethod, RequestMapping.class);
        Assertions.assertNotNull(mapping);
        Assertions.assertEquals(0, mapping.method().length);
        Assertions.assertEquals(1, mapping.path().length);
        Assertions.assertEquals("/greeting", mapping.path()[0]);
    }

    private static Method getMethod(Class<?> cls, String methodName) {
        final Method greetingMethod = Utils.getAllMethods(cls).stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .get();
        return greetingMethod;
    }

    @Test
    public void testApplicationScan() {
        final Settings settings = TestUtils.settings();
        settings.generateSpringApplicationInterface = true;
        settings.scanSpringApplication = true;
        settings.classLoader = Thread.currentThread().getContextClassLoader();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SpringTestApplication.class));
        Assertions.assertTrue(output.contains("interface RestApplication"));
        Assertions.assertTrue(output.contains("greeting(queryParams?: { name?: string; count?: number; unnamed?: string; }): RestResponse<Greeting>"));
        Assertions.assertTrue(output.contains("interface Greeting"));
    }

    @Test
    public void testPathParameters() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller1.class));
        Assertions.assertTrue(output.contains("findPet(ownerId: number, petId: number): RestResponse<Pet>"));
        Assertions.assertTrue(output.contains("uriEncoding`owners/${ownerId}/pets/${petId}`"));
        Assertions.assertTrue(output.contains("interface Pet"));
    }

    @Test
    public void testPathParametersWithoutValue() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller5.class));
        Assertions.assertTrue(output.contains("findPet(ownerId: number, petId: number): RestResponse<Pet>"));
        Assertions.assertTrue(output.contains("uriEncoding`owners2/${ownerId}/pets2/${petId}`"));
        Assertions.assertTrue(output.contains("interface Pet"));
    }

    @Test
    public void testPathParameterWithReservedWord() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithReservedWord.class));
        Assertions.assertTrue(output.contains("getLogs(_class: string): RestResponse<string[]>"));
        Assertions.assertTrue(output.contains("uriEncoding`logs/${_class}`"));
    }

    @RestController
    @RequestMapping
    public static class ControllerWithReservedWord {
        @GetMapping(value = "/logs/{class}")
        public Collection<String> getLogs(@PathVariable("class") String clazz) {
            return null;
        }
    }

    @Test
    public void testQueryParameters() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller2.class));
        Assertions.assertTrue(output.contains("echo(queryParams: { message: string; count?: number; optionalRequestParam?: number; }): RestResponse<string>"));
    }

    @Test
    public void testUnfoldedQueryParameters() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.generateClientAsService = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller2.class));
        Assertions.assertTrue(output.contains("echo(message: string, count?: number, optionalRequestParam?: number): RestResponse<string>"));
        Assertions.assertTrue(output.contains("let queryParams: any = {};"));
        Assertions.assertTrue(output.contains("queryParams.message = message;"));
        Assertions.assertTrue(output.contains("if (count != undefined) {"));
        Assertions.assertTrue(output.contains("queryParams.count = count;"));
        Assertions.assertTrue(output.contains("if (optionalRequestParam != undefined)"));
        Assertions.assertTrue(output.contains("queryParams.optionalRequestParam = optionalRequestParam;"));
        Assertions.assertTrue(output.contains("return this.httpClient.request({ method: \"GET\", url: uriEncoding`echo`, queryParams: queryParams });"));
    }

    @Test
    public void testUnfoldedQueryParametersWithSkipOptionalParams() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.generateClientAsService = true;
        settings.skipNullValuesForOptionalServiceArguments = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller2.class));
        Assertions.assertTrue(output.contains("echo(message: string, count?: number, optionalRequestParam?: number): RestResponse<string>"));
        Assertions.assertTrue(output.contains("let queryParams: any = {};"));
        Assertions.assertTrue(output.contains("queryParams.message = message;"));
        Assertions.assertTrue(output.contains("if (count != null) {"));
        Assertions.assertTrue(output.contains("queryParams.count = count;"));
        Assertions.assertTrue(output.contains("if (optionalRequestParam != null)"));
        Assertions.assertTrue(output.contains("queryParams.optionalRequestParam = optionalRequestParam;"));
        Assertions.assertTrue(output.contains("return this.httpClient.request({ method: \"GET\", url: uriEncoding`echo`, queryParams: queryParams });"));
    }

    @Test
    public void testAllOptionalQueryParameters() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller7.class));
        Assertions.assertTrue(output.contains("echo(queryParams?: { message?: string; }): RestResponse<string>"));
    }

    @Test
    public void testEntityParameter() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller3.class));
        Assertions.assertTrue(output.contains("setEntity(data: Data1): RestResponse<void>"));
        Assertions.assertTrue(output.contains("interface Data1"));
    }

    @Test
    public void testReturnType() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller4.class));
        Assertions.assertTrue(output.contains("getEntity(): RestResponse<Data2>"));
        Assertions.assertTrue(output.contains("interface Data2"));
    }

    @Test
    public void testGenerics() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller6.class));
        Assertions.assertTrue(output.contains("doSomething(input: number[]): RestResponse<{ [P in Controller6Enum]?: any }[]>"));
        Assertions.assertTrue(output.contains("type Controller6Enum"));
    }

    @Test
    public void testInheritance() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller6.class));
        Assertions.assertTrue(output.contains("doSomethingElse(id: number): RestResponse<number>"));
        Assertions.assertTrue(output.contains("doSomethingElseAgain(): RestResponse<number>"));
        Assertions.assertTrue(output.contains("uriEncoding`test/c`"));
        Assertions.assertFalse(output.contains("uriEncoding`test/b`"));
    }

    @Test
    public void testCustomQueryParameters() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.springCustomQueryParameterAnnotations = Arrays.asList(CustomRequestParam.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller8.class));
        Assertions.assertTrue(output.contains("echo(queryParams?: { message?: string; }): RestResponse<string>"));
    }

    @Test
    public void testCustomRequestBody() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.springCustomRequestBodyAnnotations = Arrays.asList(CustomRequestBody.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller9.class));
        Assertions.assertTrue(output.contains("setEntity(data: Data1): RestResponse<void>"));
        Assertions.assertTrue(output.contains("interface Data1"));
    }

    @Test
    public void testCustomRestMethodBuilder() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.customRestMethodBuilder = new CustomRestMethodBuilder();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller3.class));
        Assertions.assertTrue(output.contains("testName(data: Data1, queryParams: { testParam1: string; testParam2?: number; }): RestResponse<void>"));
        Assertions.assertTrue(output.contains("interface Data1"));
    }

    @Test
    public void testHeadersParameter() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.restHeaderArgumentsParsed = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller10.class));
        Assertions.assertTrue(output.contains("sendHeaders(headers: { ownerId: number; petId?: string; }): RestResponse<void>"));
    }

    @Test
    public void testHeadersParameterWithQueryParameter() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.restHeaderArgumentsParsed = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller11.class));
        Assertions.assertTrue(output.contains("sendHeadersAndQueryParams(queryParams: { petId: string; }, headers: { ownerId: number; }): RestResponse<void>"));
    }

    @Test
    public void testHeadersServiceWithNonValidHeaderNames() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.restHeaderArgumentsParsed = true;
        settings.generateClientAsService = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller12.class));
        Assertions.assertTrue(output.contains("sendHeaders(ownerId: number, petId?: string, normalHeader: number): RestResponse<void>"));
        Assertions.assertTrue(output.contains("let headers: any = {};"));
        Assertions.assertTrue(output.contains("headers[\"owner-id\"] = ownerId;"));
        Assertions.assertTrue(output.contains("if (petId != undefined)"));
        Assertions.assertTrue(output.contains("headers[\"pet-id\"] = petId;"));
        Assertions.assertTrue(output.contains("headers.normalHeader = normalHeader;"));
        Assertions.assertTrue(output.contains("return this.httpClient.request({ method: \"GET\", url: uriEncoding`headers12`, headers: headers });"));
    }

    @RestController
    @RequestMapping("/owners/{ownerId}")
    public static class Controller1 {
        @GetMapping("/pets/{petId}")
        public Pet findPet(
                @PathVariable("ownerId") Long ownerId,
                @PathVariable(name = "petId") Long petId
        ) {
            return null;
        }
    }

    @RestController
    public static class Controller2 {
        @RequestMapping("/echo")
        public String echo(
                @RequestParam("message") String message,
                @RequestParam(name = "count", defaultValue = "1") Integer count,
                @RequestParam(required = false) Integer optionalRequestParam
        ) {
            return message;
        }
    }

    @RestController
    public static class Controller7 {
        @RequestMapping("/echo2")
        public String echo(
                @RequestParam(required = false) String message
        ) {
            return message;
        }
    }

    @RestController
    public static class Controller8 {
        @RequestMapping("/echo3")
        public String echo(
                @CustomRequestParam String message
        ) {
            return message;
        }
    }

    @RestController
    public static class Controller9 {
        @RequestMapping(path = "/data2", method = RequestMethod.PUT)
        public void setEntity(@CustomRequestBody Data1 data) {
        }
    }

    @Test
    public void testQueryParametersWithModel() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithModelAttribute.class));
        Assertions.assertTrue(output.contains("echoWithModelAttribute(queryParams?: { message?: string; }): RestResponse<string>"));
    }

    @RestController
    public static class ControllerWithModelAttribute {
        @RequestMapping("/echoWithModelAttribute")
        public String echoWithModelAttribute(@ModelAttribute FilterParams nested) {
            return nested.getMessage();
        }

        static class FilterParams {
            private String message;
            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }
        }
    }

    @RestController
    public static class Controller3 {
        @RequestMapping(path = "/data1", method = RequestMethod.PUT)
        public void setEntity(@RequestBody Data1 data) {
        }
    }

    @RestController
    public static class Controller4 {
        @RequestMapping(path = "/data2", method = RequestMethod.GET)
        public ResponseEntity<Data2> getEntity() {
            return null;
        }
    }

    @RestController
    @RequestMapping("/owners2/{ownerId}")
    public static class Controller5 {
        @GetMapping("/pets2/{petId}")
        public Pet findPet(
                @PathVariable Long ownerId,
                @PathVariable Long petId
        ) {
            return null;
        }
    }

    private enum Controller6Enum {
        A,
        B
    }

    @RestController
    @RequestMapping("/test")
    public static class Controller6 extends Controller6Super<Controller6Enum, Integer> {
        @Override
        int doSomethingElse(@PathVariable long id) {
            return 3;
        }

        @GetMapping("/c")
        @Override
        int doSomethingElseAgain() {
            return super.doSomethingElseAgain();
        }
    }

    static abstract class Controller6Super<A extends Enum<A>, B> {

        @PostMapping("a")
        List<Map<A, ?>> doSomething(@RequestBody List<B> input) {
            return null;
        }

        @GetMapping("/{id}")
        int doSomethingElse(@PathVariable long id) {
            return 1;
        }

        @GetMapping("/b")
        int doSomethingElseAgain() {
            return 1;
        }
    }

    @RestController
    public static class Controller10 {
        @GetMapping("/headers1")
        public void sendHeaders(
                @RequestHeader Long ownerId,
                @RequestHeader(required = false) String petId
        ) {}
    }

    @RestController
    public static class Controller11 {
        @GetMapping("/headersWithQueryParams1")
        public void sendHeadersAndQueryParams(
                @RequestHeader Long ownerId,
                @RequestParam String petId
        ) {}
    }

    @RestController
    public static class Controller12 {
        @GetMapping("/headers12")
        public void sendHeaders(
                @RequestHeader(name = "owner-id") Long ownerId,
                @RequestHeader(name = "pet-id", required = false) String petId,
                @RequestHeader Long normalHeader) {}
    }

    public static class Pet {
    }

    public static class Data1 {
    }

    public static class Data2 {
    }

    @Test
    public void testUnwrapping() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.customTypeMappings.put("cz.habarta.typescript.generator.spring.SpringTest$Wrapper<T>", "Unwrap<T>");
        settings.importDeclarations.add("import { Unwrap } from './unwrap'");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithWrapper.class));
        Assertions.assertTrue(output.contains("getEntity(): RestResponse<Unwrap<string>>"));
    }

    @Test
    public void testUnwrappingNew() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.customTypeMappings.put("cz.habarta.typescript.generator.spring.SpringTest$Wrapper<T>", "T");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithWrapper.class));
        Assertions.assertTrue(output.contains("getEntity(): RestResponse<string>"));
    }

    @RestController
    public static class ControllerWithWrapper {
        @RequestMapping(path = "/data", method = RequestMethod.GET)
        public Wrapper<String> getEntity() {
            return null;
        }
    }

    public static class Wrapper<T> {
        public T value;
    }

    @Test
    public void testGenericController() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ConcreteGenerticController.class));
        Assertions.assertTrue(output.contains("post(input: string): RestResponse<number>"));
    }

    @RestController
    public static abstract class AbstractGenerticController<T, R> {
        @PostMapping("/generic")
        public R post(@RequestBody T input) {
            return map(input);
        }

        abstract protected R map(T input);
    }

    public static class ConcreteGenerticController extends AbstractGenerticController<String, Integer> {
        protected Integer map(String input) {
            return input.length();
        }
    }

    @Test
    public void testPageableController() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(PageableController.class));
        Assertions.assertTrue(output.contains(" post(queryParams?: { page?: number; size?: number; sort?: string; }): RestResponse<Page<string>>"));
    }

    @RestController
    public static abstract class PageableController {
        @GetMapping("/pageable")
        public Page<String> post(Pageable page) {
            return null;
        }
    }

    @Test
    public void testDoubleGenericController() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DoubleGenericController.class));
        Assertions.assertTrue(output.contains(" get(): RestResponse<string[]>"));
    }

    @RestController
    public class DoubleGenericController  {
        @GetMapping("/generic2")
        public ResponseEntity<List<String>> get () {
            return ResponseEntity.ok(Arrays.asList( "a" , "b" , "c" ));
        }
    }

    @Test
    public void testCustomControllerAnnotaion() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(CustomAnnotatedController.class));
        Assertions.assertTrue(output.contains("getText(): RestResponse<string>"));
    }

    @MyRestController
    public class CustomAnnotatedController {
        @GetMapping("/text")
        public String getText() {
            return "";
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Component
    public @interface MyRestController {
    }

    @Test
    public void testUrlTrailingSlash() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(TestUrlTrailingSlashController.class));
        Assertions.assertTrue(Pattern.compile("response\\(\\):.*\\n.*uriEncoding`controller/`").matcher(output).find());
        Assertions.assertTrue(Pattern.compile("response2\\(\\):.*\\n.*uriEncoding`controller`").matcher(output).find());
    }

    @RestController
    @RequestMapping("/controller")
    public class TestUrlTrailingSlashController {
        @GetMapping("/")
        public void response() {
        }
        @GetMapping("")
        public void response2() {
        }
    }

    @Test
    public void testMultiValueMapRequestParam() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithMultiValueMap.class));
        Assertions.assertTrue(output.contains("search(queryParams?: { [index: string]: any }): RestResponse<string>"));
    }

    @RestController
    public class ControllerWithMultiValueMap {
        @GetMapping("/search")
        public String search(@RequestParam MultiValueMap<String, String> params) {
            return "";
        }
    }

    @Test
    public void testSwaggerExclude() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithSwaggerIgnore.class));
        Assertions.assertTrue(!output.contains("shouldBeExcluded"));
    }

    @RestController
    public class ControllerWithSwaggerIgnore {
        @ApiOperation(value = "", hidden = true)
        @GetMapping("/test")
        public String shouldBeExcluded() {
            return "";
        }
    }

    @Test
    public void testSwagger3Exclude() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithSwagger3Ignore.class));
        Assertions.assertTrue(!output.contains("shouldBeExcluded"));
    }

    @RestController
    public class ControllerWithSwagger3Ignore {
        @Operation(hidden = true)
        @GetMapping("/test")
        public String shouldBeExcluded() {
            return "";
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface CustomRequestParam {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface CustomRequestBody {

    }

    public class CustomRestMethodBuilder implements RestMethodBuilder{

        @Override
        public RestMethodModel build(Class<?> originClass, String name, Type returnType,
                                     Method originalMethod, Class<?> rootResource, String httpMethod,
                                     String path, List<MethodParameterModel> pathParams, List<RestParam> queryParams,
                                     MethodParameterModel entityParam, List<String> comments, List<RestParam> headers) {

            RestParam.Single queryParam1 = new RestParam.Single(new MethodParameterModel("testParam1", String.class), true);
            RestParam.Single queryParam2 = new RestParam.Single(new MethodParameterModel("testParam2", Integer.class), false);

            return new RestMethodModel(originClass, "testName", returnType, originalMethod, rootResource, httpMethod, path,
                    pathParams, Arrays.asList(queryParam1, queryParam2), entityParam, comments, headers);
        }
    }

}
