package org.ulitzky.devices.service.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class DeviceMapperTest {

    @Autowired
    private DeviceMapper deviceMapper;

    @Test
    public void testMapValidUUIDToString() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When
        String mapped = deviceMapper.map(uuid);

        // Then
        assertNotNull(mapped);
        assertEquals(uuid, UUID.fromString(mapped));
    }

    @Test
    public void testMapValidUStringToUUID() {
        // Given
        String input = UUID.randomUUID().toString();

        // When
        UUID uuid = deviceMapper.map(input);

        // Then
        assertNotNull(uuid);
        assertEquals(input, uuid.toString());
    }

    @Test
    public void testMapInvalidUStringToUUID() {
        // Given
        String input = UUID.randomUUID().toString().substring(10);

        // When
        UUID result = deviceMapper.map(input);

        // Then
        assertNull(result);
    }



}
