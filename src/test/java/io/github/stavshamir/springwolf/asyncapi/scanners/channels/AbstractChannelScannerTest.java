package io.github.stavshamir.springwolf.asyncapi.scanners.channels;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.stavshamir.springwolf.asyncapi.scanners.components.ComponentsScanner;
import io.github.stavshamir.springwolf.asyncapi.types.channel.Channel;
import io.github.stavshamir.springwolf.asyncapi.types.channel.operation.Operation;
import io.github.stavshamir.springwolf.asyncapi.types.channel.operation.bindings.kafka.KafkaOperationBinding;
import io.github.stavshamir.springwolf.asyncapi.types.channel.operation.message.Message;
import io.github.stavshamir.springwolf.asyncapi.types.channel.operation.message.PayloadReference;
import io.github.stavshamir.springwolf.schemas.DefaultSchemasService;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestChannelScanner.class, DefaultSchemasService.class})
public class AbstractChannelScannerTest {

    @Autowired
    private TestChannelScanner channelScanner;

    @MockBean
    private ComponentsScanner componentsScanner;

    private void setClassToScan(Class<?> classToScan) {
        Set<Class<?>> classesToScan = singleton(classToScan);
        when(componentsScanner.scanForComponents(anyString())).thenReturn(classesToScan);
    }

    @Test
    public void scan_componentHasNoListenerMethods() {
        setClassToScan(ClassWithoutListenerAnnotation.class);

        Map<String, Channel> channels = channelScanner.scan();

        assertThat(channels).isEmpty();
    }

    @Test
    public void scan_componentHasListenerMethod() {
        // Given a class with methods annotated with KafkaListener, whose topics attribute is hard coded
        setClassToScan(ClassWithListenerAnnotation.class);

        // When scan is called
        Map<String, Channel> actualChannels = channelScanner.scan();

        // Then the returned collection contains the channel
        Message message = Message.builder()
                .name(SimpleFoo.class.getName())
                .title(SimpleFoo.class.getSimpleName())
                .payload(PayloadReference.fromModelName(SimpleFoo.class.getSimpleName()))
                .build();

        Operation operation = Operation.builder()
                .bindings(ImmutableMap.of("kafka", KafkaOperationBinding.withGroupId("test-group")))
                .message(message)
                .build();

        Channel expectedChannel = Channel.builder().publish(operation).build();

        assertThat(actualChannels)
                .containsExactly(Maps.immutableEntry("test-channel", expectedChannel));
    }

    private static class ClassWithoutListenerAnnotation {

        private void methodWithoutAnnotation() {
        }

    }

    private static class ClassWithListenerAnnotation {

        @TestChannelListener
        private void methodWithAnnotation(SimpleFoo payload) {
        }

        private void methodWithoutAnnotation() {
        }

    }

    @Data
    @NoArgsConstructor
    private static class SimpleFoo {
        private String s;
        private boolean b;
    }

    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestChannelListener { }
}

