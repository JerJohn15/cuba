/*
 * Copyright (c) 2008-2016 Haulmont.
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
 *
 */

package com.haulmont.cuba.gui.app.security.ds;

import com.google.common.base.Predicate;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.app.security.role.edit.PermissionValue;
import com.haulmont.cuba.gui.config.PermissionConfig;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.impl.CollectionDatasourceImpl;
import com.haulmont.cuba.gui.app.security.entity.OperationPermissionTarget;
import com.haulmont.cuba.gui.app.security.entity.PermissionVariant;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.Permission;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityPermissionTargetsDatasource extends CollectionDatasourceImpl<OperationPermissionTarget, String> {

    protected List<OperationPermissionTarget> targets;

    protected Predicate<OperationPermissionTarget> permissionsFilter;

    protected CollectionDatasource<Permission, UUID> permissionDs;

    protected UserSessionSource userSessionSource;

    public EntityPermissionTargetsDatasource() {
        userSessionSource = AppBeans.get(UserSessionSource.NAME);
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    protected void loadData(Map<String, Object> params) {
        if (permissionDs == null)
            return;

        if (targets == null) {
            targets = new ArrayList<>();
            PermissionConfig permissionConfig = AppBeans.get(PermissionConfig.class);
            List<OperationPermissionTarget> entities = permissionConfig.getEntities(userSessionSource.getLocale());
            for (OperationPermissionTarget target : entities) {
                try {
                    OperationPermissionTarget cloneTarget = target.clone();
                    loadPermissionVariants(cloneTarget);
                    attachListener(cloneTarget);
                    targets.add(cloneTarget);
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        data.clear();

        for (OperationPermissionTarget target : targets) {
            if ((permissionsFilter == null) || (permissionsFilter.apply(target))) {
                data.put(target.getId(), target);
            }
        }
    }

    private void loadPermissionVariants(OperationPermissionTarget target) {
        for (Permission p : permissionDs.getItems()) {
            String permissionTargetString = p.getTarget();
            if (StringUtils.isNotEmpty(permissionTargetString)) {
                int delimeterIndex = permissionTargetString.lastIndexOf(Permission.TARGET_PATH_DELIMETER);
                if (delimeterIndex >= 0) {
                    String variant = permissionTargetString.substring(delimeterIndex + 1);
                    String permissionTarget = permissionTargetString.substring(0, delimeterIndex);

                    if (StringUtils.equals(permissionTarget, target.getPermissionValue())) {
                        PermissionVariant permissionVariant = getPermissionVariant(p);
                        if (EntityOp.CREATE.getId().equals(variant)) {
                            target.setCreatePermissionVariant(permissionVariant);
                        } else if (EntityOp.READ.getId().equals(variant)) {
                            target.setReadPermissionVariant(permissionVariant);
                        } else if (EntityOp.UPDATE.getId().equals(variant)) {
                            target.setUpdatePermissionVariant(permissionVariant);
                        } else if (EntityOp.DELETE.getId().equals(variant)) {
                            target.setDeletePermissionVariant(permissionVariant);
                        }
                    }
                }
            }
        }
    }

    private PermissionVariant getPermissionVariant(Permission permission) {
        if (permission.getValue() == PermissionValue.ALLOW.getValue())
            return PermissionVariant.ALLOWED;
        else if (permission.getValue() == PermissionValue.DENY.getValue())
            return PermissionVariant.DISALLOWED;
        else
            return PermissionVariant.NOTSET;
    }

    public Predicate<OperationPermissionTarget> getFilter() {
        return permissionsFilter;
    }

    public void setFilter(Predicate<OperationPermissionTarget> filter) {
        this.permissionsFilter = filter;
    }

    public CollectionDatasource<Permission, UUID> getPermissionDs() {
        return permissionDs;
    }

    public void setPermissionDs(CollectionDatasource<Permission, UUID> permissionDs) {
        this.permissionDs = permissionDs;
    }
}