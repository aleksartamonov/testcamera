package com.compscicenter.aleksart.test2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;
import android.content.SharedPreferences;

import junit.framework.TestCase;

import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by sergej on 4/18/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class DescriptorTest extends TestCase {
    @Mock
    Context mMockContext;


    public void testGetStr() throws Exception {
        assertEquals("ABA", Descriptor.getStr());

    }

    public void setUp() throws Exception {
        super.setUp();
    }


}