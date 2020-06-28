package de.dennisguse.opentracks.util;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class UUIDUtilsTest {

    @Test
    public void test() {
        UUID uuid = UUID.randomUUID();

        byte[] bytes = UUIDUtils.toBytes(uuid);
        UUID output = UUIDUtils.fromBytes(bytes);

        Assert.assertEquals(uuid, output);
    }
}