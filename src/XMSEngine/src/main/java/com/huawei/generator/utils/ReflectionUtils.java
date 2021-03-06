/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.huawei.generator.utils;

import static com.huawei.generator.gen.AstConstants.GENERIC_PREFIX;
import static com.huawei.generator.gen.AstConstants.XMS_UTILS;

import com.huawei.generator.ast.AssignNode;
import com.huawei.generator.ast.CallNode;
import com.huawei.generator.ast.CastExprNode;
import com.huawei.generator.ast.ConstantNode;
import com.huawei.generator.ast.DeclareNode;
import com.huawei.generator.ast.GetField;
import com.huawei.generator.ast.MethodNode;
import com.huawei.generator.ast.NewArrayNode;
import com.huawei.generator.ast.ReturnNode;
import com.huawei.generator.ast.StatementNode;
import com.huawei.generator.ast.TypeNode;
import com.huawei.generator.ast.VarNode;
import com.huawei.generator.gen.AstConstants;
import com.huawei.generator.json.JMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle reflection type
 *
 * @since 2020-05-12
 */
public class ReflectionUtils {
    /**
     * A pair dedicating a parameter type with upper bound class if generic.
     */
    public static class TypePair {
        private boolean isGeneric;

        private String upperbound;

        private TypePair(boolean isGeneric, String upperbound) {
            this.isGeneric = isGeneric;
            this.upperbound = upperbound;
        }

        /**
         * is generic or not.
         * 
         * @return true if is generic; else otherwise
         */
        public boolean isGeneric() {
            return isGeneric;
        }

        /**
         * get type upper bound.
         * 
         * @return type name
         */
        public String getUpperbound() {
            return upperbound;
        }
    }

    /**
     * get parameter type upper bound.
     * 
     * @param methodNode the method node.
     * @param index parameter index.
     * @return return parameter of index <isGeneric, type>
     */
    public static TypePair getParameterType(MethodNode methodNode, int index) {
        TypeNode typeNode = methodNode.parameters().get(index);
        // R
        String typeName = typeNode.getTypeName();
        // XR
        String xTypeName = GENERIC_PREFIX + typeName;
        String superClass = AstConstants.OBJECT + ".class";
        // find generic upper bound
        boolean isGeneric = false;
        // check whether defined in the generic types of the outer X class
        if (methodNode.parent().outerClass().generics() != null) {
            for (TypeNode generic : methodNode.parent().outerClass().generics()) {
                if (!xTypeName.equals(generic.getTypeName()) && !typeName.equals(generic.getTypeName())) {
                    continue;
                }
                if (generic.getSuperClass() != null && generic.getSuperClass().size() != 0) {
                    superClass = generic.getSuperClass().get(0).getTypeName() + ".class";
                }
                isGeneric = true;
                break;
            }
        }

        // check whether defined in the generic types of the method
        if (methodNode.getMethodGenerics() != null) {
            for (TypeNode generic : methodNode.getMethodGenerics()) {
                if (!typeName.equals(generic.getTypeName())) {
                    continue;
                }
                if (generic.getSuperClass() != null && generic.getSuperClass().size() != 0) {
                    superClass = generic.getSuperClass().get(0).getTypeName() + ".class";
                    isGeneric = true;
                    break;
                }
            }
        }

        if (!isGeneric) {
            superClass = typeName + ".class";
        }

        return new TypePair(isGeneric, superClass);
    }

    /**
     * check method node has generic parameters or not.
     * 
     * @param methodNode the method node.
     * @return return true if methodNode has generic parameters. false otherwise.
     */
    public static boolean hasGenericParameters(MethodNode methodNode) {
        if (methodNode.parameters() == null || methodNode.parameters().size() == 0) {
            return false;
        }

        for (int index = 0; index < methodNode.parameters().size(); index++) {
            TypePair tp = getParameterType(methodNode, index);
            if (tp.isGeneric) {
                return true;
            }
        }

        return false;
    }

    /**
     * invoke handleInvokeBridgeReturnValue without cast.
     * 
     * @param resultHandle the result handle.
     * @param isH true if HMS; else otherwise.
     * @return return call node of handleInvokeBridgeReturnValue, without cast.
     */
    private static StatementNode handleInvokeBridgeMethodReturnValueWithoutCast(VarNode resultHandle, boolean isH) {
        // init params
        List<StatementNode> params = new ArrayList<>();
        params.add(resultHandle);
        params.add(ConstantNode.create("boolean", String.valueOf(isH)));

        // call AstConstants.XMS_UTILS.handleInvokeBridgeReturnValue
        return CallNode.create(VarNode.create(AstConstants.XMS_UTILS), "handleInvokeBridgeReturnValue", params);
    }

    /**
     * invoke handleInvokeBridgeReturnValue without castExpr.
     * 
     * @param resultHandle the result handle.
     * @param methodNode keep same argument with handleInvokeBridgeMethodReturnValue.
     * @param isH true if HMS; else otherwise.
     * @return return call node of handleInvokeBridgeReturnValue, with cast.
     */
    private static StatementNode handleInvokeBridgeMethodReturnValue(VarNode resultHandle, MethodNode methodNode,
        boolean isH) {
        return CastExprNode.create(TypeNode.create(methodNode.returnType().getTypeName()),
            handleInvokeBridgeMethodReturnValueWithoutCast(resultHandle, isH));
    }

    private static void genTypeAssignment(List<StatementNode> block, MethodNode methodNode) {
        // types[i] = types i
        for (int index = 0; index < methodNode.parameters().size(); index++) {
            TypePair tp = getParameterType(methodNode, index);
            if (!tp.isGeneric()) {
                // if tp.upperbound == G/H, change upperbound to X
                // get className
                String suffix = ".class";
                String className = tp.getUpperbound().substring(0, tp.getUpperbound().lastIndexOf(suffix));
                // HName -> XName
                if (TypeUtils.isHmsType(className)) {
                    GlobalMapping globalMapping = GlobalMapping.getHmappings().get(className);
                    if (globalMapping == null) {
                        continue;
                    }
                    // use TypeNode to erase generic.
                    tp.upperbound = TypeNode.create(globalMapping.getX()).getTypeName() + suffix;
                }
                if (TypeUtils.isGmsType(className)) { // GName -> XName
                    tp.upperbound = TypeNode.create(className).toX().getTypeName() + suffix;
                }
            }
            block.add(AssignNode.create(VarNode.create("types[" + index + "]"), VarNode.create(tp.getUpperbound())));
        }
    }

    /**
     * invoke bridge method with method parameters.
     * 
     * @param methodNode the method node.
     * @return statement nodes of generate bridge method parameters.
     */
    private static List<StatementNode> genInvokeBridgeParametersFromParameters(MethodNode methodNode) {
        List<StatementNode> block = new ArrayList<>();
        int paraSize = methodNode.parameters().size();

        // java.lang.Object[] params = new java.lang.Object[size]
        block.add(AssignNode.create(DeclareNode.create(TypeNode.create(AstConstants.OBJECT + "[]"), "params"),
            NewArrayNode.create(TypeNode.create(AstConstants.OBJECT), String.valueOf(paraSize))));

        // java.lang.Class[] types = new java.lang.Class[size]
        block.add(AssignNode.create(DeclareNode.create(TypeNode.create("java.lang.Class[]"), "types"),
            NewArrayNode.create(TypeNode.create("java.lang.Class"), String.valueOf(paraSize))));

        // params[i] = params i;
        for (int index = 0; index < methodNode.parameters().size(); index++) {
            block.add(
                AssignNode.create(VarNode.create("params[" + index + "]"), VarNode.create(methodNode.paramAt(index))));
        }

        // types[i] = types i
        genTypeAssignment(block, methodNode);

        return block;
    }

    /**
     * generate invoke bridgeMethod block.
     *
     * @param methodNode the method node.
     * @param receiver the invoke target.
     * @param bridgedMethodName method name of call target.
     * @param isH HMS if true, GMS otherwise.
     */
    public static List<StatementNode> genInvokeBridgeMethodBlock(MethodNode methodNode, StatementNode receiver,
        String bridgedMethodName, boolean isH) {
        // call "invokeBridgeMethod"
        List<StatementNode> xParams = new ArrayList<>();
        xParams.add(receiver);
        xParams.add(ConstantNode.create("java.lang.String", bridgedMethodName));
        xParams.add(VarNode.create("params"));
        xParams.add(VarNode.create("types"));
        xParams.add(ConstantNode.create("boolean", String.valueOf(isH)));
        CallNode callBridge = CallNode.create(VarNode.create(AstConstants.XMS_UTILS), "invokeBridgeMethod", xParams);

        // handle invokeBridgeMethod parameters
        List<StatementNode> block = new ArrayList<>(genInvokeBridgeParametersFromParameters(methodNode));
        // return void
        if (methodNode.returnType() == null || methodNode.isReturnVoid()) {
            block.add(callBridge);
            return block;
        }

        // Object result = invokeBridgeMethod()
        AssignNode result =
            AssignNode.create(DeclareNode.create(TypeNode.create(AstConstants.OBJECT), "result"), callBridge);
        block.add(result);

        // handle return value
        StatementNode returnValue = handleInvokeBridgeMethodReturnValue(VarNode.create("result"), methodNode, isH);
        block.add(ReturnNode.create(returnValue));
        return block;
    }

    private static List<StatementNode> genProtectedMethodParameters(JMethod jMethod, List<StatementNode> args) {
        List<StatementNode> block = new ArrayList<>();
        int paraSize = jMethod.parameterTypes().size();

        // java.lang.Object[] params = new java.lang.Object[size]
        block.add(AssignNode.create(DeclareNode.create(TypeNode.create(AstConstants.OBJECT + "[]"), "params"),
            NewArrayNode.create(TypeNode.create(AstConstants.OBJECT), String.valueOf(paraSize))));

        // java.lang.Class[] types = new java.lang.Class[size]
        block.add(AssignNode.create(DeclareNode.create(TypeNode.create("java.lang.Class[]"), "types"),
            NewArrayNode.create(TypeNode.create("java.lang.Class"), String.valueOf(paraSize))));

        // params[i] = params i;
        for (int index = 0; index < jMethod.parameterTypes().size(); index++) {
            block.add(AssignNode.create(VarNode.create("params[" + index + "]"), args.get(index)));
        }

        // types[i] = types i
        for (int index = 0; index < jMethod.parameterTypes().size(); index++) {
            block.add(AssignNode.create(VarNode.create("types[" + index + "]"),
                GetField.create(VarNode.create(jMethod.parameterTypes().get(index).type()), "class")));
        }

        return block;
    }

    /**
     * generate invoke protect method block.
     *
     * @param body generated body
     * @param methodNode the method node.
     * @param jMethod invoke JMethod
     * @param receiver invoke receiver
     * @param args invoke args
     * @param isH HMS if true, GMS otherwise.
     * @return return Call Node.
     */
    public static CallNode genInvokeProtectMethod(List<StatementNode> body, MethodNode methodNode, JMethod jMethod,
        StatementNode receiver, List<StatementNode> args, boolean isH) {
        String className = methodNode.parent().getGType().getTypeName();
        if (isH) {
            className = methodNode.parent().getHType().getTypeName();
        }
        String methodName = jMethod.name();
        body.addAll(genProtectedMethodParameters(jMethod, args));
        List<StatementNode> invokeParams = new ArrayList<>();
        invokeParams.add(receiver);
        invokeParams.add(GetField.create(VarNode.create(className), "class"));
        invokeParams.add(ConstantNode.create("java.lang.String", methodName));
        invokeParams.add(VarNode.create("types"));
        invokeParams.add(VarNode.create("params"));
        // invoke protectMethod
        return CallNode.create(VarNode.create(XMS_UTILS), "invokeProtectMethod", invokeParams);
    }
}
