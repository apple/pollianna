/*
 * Copyright (c) 2023-2025 Apple Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apple.pollianna;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A dynamic bean implementation that projects a reduced view upon a static bean implementation
 * by filtering out select attributes.
 *
 * A `DynamicSeed` bean attribute can be the compound of a getter method in the projected bean interface
 * and a getter method of that method's return type. Example:
 * - Method `getPause` returns a `LongDurationRecord` that has a method `getMax`.
 * - This constitutes a dynamic bean attribute "PauseMax".
 *
 * Only complete attribute names (i.e. "PauseMax", but not "Pause" or "Max")
 * are exposed as attributes by the dynamic bean.
 */
public class DynamicSeed extends Seed implements DynamicMBean {

    private final String beanName;

    @Override
    public String beanName() {
        return beanName;
    }

    private final ArrayList<MBeanAttributeInfo> beanAttributes = new ArrayList<MBeanAttributeInfo>();
    private final Seed staticSeed;

    // Maps complete attribute names to getter methods in the projected bean interface
    private final Map<String, Method> getters1 = new HashMap<String, Method>();

    // Maps complete attribute names to getter methods in the return types of the above getters
    private final Map<String, Method> getters2 = new HashMap<String, Method>();

    private MBeanInfo beanInfo;

    // Implement DynamicMBean
    public MBeanInfo getMBeanInfo() {
        return beanInfo;
    }

    private MBeanAttributeInfo createAttribute(String name, Class type) {
        final String description = name + " : " + type.getName();
        final boolean isReadable = true;
        final boolean isWritable = false;
        final boolean isIs = false;
        return new MBeanAttributeInfo(name, type.getName(), description, isReadable, isWritable, isIs);
    }

    private static final String GETTER_PREFIX = "get";

    private void addAttributes(ArrayList<MBeanAttributeInfo> attributes, Class getterInterface) {
        if (!getterInterface.isInterface()) {
            return;
        }
        for (Method method1 : getterInterface.getDeclaredMethods()) {
            if (method1.getName().startsWith(GETTER_PREFIX)) {
                final String name1 = method1.getName().substring(GETTER_PREFIX.length());
                final Class type1 = method1.getReturnType();
                boolean isSimpleType = true;
                for (Method method2 :  type1.getMethods()) {
                    final String method2Name = method2.getName();
                    if (method2Name.startsWith(GETTER_PREFIX) &&
                            !method2Name.equals("getClass") && !method2Name.equals("getObjectName")) {
                        final String name2 = name1 + method2.getName().substring(GETTER_PREFIX.length());
                        attributes.add(createAttribute(name2, method2.getReturnType()));
                        getters1.put(name2, method1);
                        getters2.put(name2, method2);
                        isSimpleType = false;
                    }
                }
                if (isSimpleType) {
                    attributes.add(createAttribute(name1, type1));
                    getters1.put(name1, method1);
                }
            }
        }
        for (Class i : getterInterface.getInterfaces()) {
            addAttributes(attributes, i);
        }
    }

    /**
     * Create a dynamic bean with getter methods from the given interface, or less, if filtered,
     * and delegate invocations to the given instance.
     *
     * @param getterInterface the interface specifying all possible getter methods
     * @param staticSeed the instance to reflectively invoke the interface methods upon
     */
    public DynamicSeed(Class getterInterface, Seed staticSeed) {
        this.staticSeed = staticSeed;
        this.beanName = getterInterface.getSimpleName().replace("MXBean", "");
        addAttributes(beanAttributes, getterInterface);

        final MBeanConstructorInfo[] beanConstructors = new MBeanConstructorInfo[1];
        beanConstructors[0] = new MBeanConstructorInfo("PolliannaDynamicMBean()", this.getClass().getConstructors()[0]);

        this.beanInfo = new MBeanInfo(beanName(),
            "dynamic " + beanName,
            beanAttributes.toArray(new MBeanAttributeInfo[0]), // Java 8
            beanConstructors,
            new MBeanOperationInfo[0],
            new MBeanNotificationInfo[0]);
    }

    /**
     * Restrict the attributes of this dynamic bean to those that match the given set of names.
     * @param includedAttributeNames set of names that the names of all visible attributes are part of
     */
    public void setIncludedAttributeNames(Set<String> includedAttributeNames) {
        final Stream<MBeanAttributeInfo> filteredBeanAttributes =
            beanAttributes.stream().filter(a -> includedAttributeNames.contains(a.getName()));

        beanInfo = new MBeanInfo(beanName,
            beanInfo.getDescription(),
            filteredBeanAttributes.toArray(MBeanAttributeInfo[]::new),
            beanInfo.getConstructors(),
            beanInfo.getOperations(),
            beanInfo.getNotifications());
    }

    @Override
    protected List<Aggregator> aggregators() { return staticSeed.aggregators(); }

    // Implement DynamicMBean
    public Object getAttribute(String attributeName) throws AttributeNotFoundException {
        if (attributeName == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name cannot be null"),
                "Cannot invoke a getter of " + beanName + " with null attribute name");
        }
        final Method getter1 = getters1.get(attributeName);
        if (getter1 == null) {
            throw(new AttributeNotFoundException(
                "Failed to find " + attributeName + " attribute in " + beanName));
        }
        try {
            Object result = getter1.invoke(staticSeed);
            final Method getter2 = getters2.get(attributeName);
            if (getter2 != null) {
                result = getter2.invoke(result);
            }
            return result;
        } catch (Exception e) {
            throw(new AttributeNotFoundException(
                "Failed to use " + attributeName + " attribute in " + beanName + ": " + e.getMessage()));
        }
    }

    // Implement DynamicMBean
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException {
        if (attribute == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Attribute cannot be null"),
                "Cannot invoke a setter of " + beanName + " with null attribute");
        }
        throw(new AttributeNotFoundException("Cannot set attribute "+ attribute.getName() + " because it is read-only"));
    }

    // Implement DynamicMBean
    public AttributeList getAttributes(String[] attributeNames) {
        if (attributeNames == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("attributeNames[] cannot be null"),
                "Cannot invoke a getter of " + beanName);
        }
        final AttributeList result = new AttributeList();
        for (String name : attributeNames){
            try {
                result.add(new Attribute(name, getAttribute(name)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // Implement DynamicMBean
    public AttributeList setAttributes(AttributeList attributes) {
        if (attributes == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("AttributeList attributes cannot be null"),
                "Cannot invoke a setter of " + beanName);
        }
        final AttributeList result = new AttributeList();
        for(Object a : attributes) {
            final Attribute attribute = (Attribute) a;
            try {
                setAttribute(attribute);
                final String name = attribute.getName();
                final Object value = getAttribute(name);
                result.add(new Attribute(name, value));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // Implement DynamicMBean
    public Object invoke(String operationName, Object[] parameters, String[] signature) throws ReflectionException {
        throw new ReflectionException(new NoSuchMethodException(operationName),
            "Cannot find the operation " + operationName + " in " + beanName);
    }
}
