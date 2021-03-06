/*
 * Copyright 2015 Johns Hopkins University
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

package org.dataconservancy.packaging.gui.presenter.impl;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.dataconservancy.dcs.util.DateUtility;
import org.dataconservancy.dcs.util.Util;
import org.dataconservancy.packaging.gui.Errors.ErrorKey;
import org.dataconservancy.packaging.gui.TextFactory;
import org.dataconservancy.packaging.gui.presenter.PackageMetadataPresenter;
import org.dataconservancy.packaging.gui.services.PackageMetadataService;
import org.dataconservancy.packaging.gui.util.DatePropertyBox;
import org.dataconservancy.packaging.gui.util.PropertyBox;
import org.dataconservancy.packaging.gui.util.RemovableLabel;
import org.dataconservancy.packaging.gui.util.TextPropertyBox;
import org.dataconservancy.packaging.gui.view.PackageMetadataView;
import org.dataconservancy.packaging.tool.api.DomainProfileStore;
import org.dataconservancy.packaging.tool.model.GeneralParameterNames;
import org.dataconservancy.packaging.tool.model.PackageMetadata;
import org.dataconservancy.packaging.tool.model.RDFTransformException;
import org.dataconservancy.packaging.tool.model.dprofile.DomainProfile;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for the screen that will handle package metadata.
 */
public class PackageMetadataPresenterImpl extends BasePresenterImpl implements PackageMetadataPresenter {
    private PackageMetadataView view;
    private PackageMetadataService packageMetadataService;
    private DomainProfileStore domainProfileStore;
    private Map<String,URI> domainProfileIdMap = new HashMap<>();

    public PackageMetadataPresenterImpl(PackageMetadataView view) {
        super(view);
        this.view = view;

        view.setPresenter(this);
    }

    @Override
    public void clear() {
        view.clearAllFields();
    }

    public Node display() {
        view.getHeaderViewHelpLink().setOnAction(arg0 -> view.showHelpPopup());

        bind();

        setExistingValues();

        //If we navigate back to the package metadata screen after creating a tree we need to disable profile selection
        if (controller.getPackageTree() != null) {
            view.getDomainProfilesComboBox().setDisable(true);
        }

        controller.getDefaultStateFileName().bind(view.getPackageNameField().getPropertyInput().textProperty());

        return view.asNode();
    }

    private void setExistingValues() {
        if (getController().getPackageState().hasPackageMetadataValues()) {
            view.clearAllFields();

            if (!Util.isEmptyOrNull(getController().getPackageState().getPackageName())) {
                view.getPackageNameField().getPropertyInput().setText(getController().getPackageState().getPackageName());
            }

            if (getController().getPackageState().getPackageMetadataValues(GeneralParameterNames.DOMAIN_PROFILE) != null &&
                !getController().getPackageState().getPackageMetadataValues(GeneralParameterNames.DOMAIN_PROFILE).isEmpty() &&
                !Util.isEmptyOrNull(getController().getPackageState().getPackageMetadataValues(GeneralParameterNames.DOMAIN_PROFILE).get(0))) {
                URI domainProfileURI = null;
                try {
                    domainProfileURI = new URI(getController().getPackageState().getPackageMetadataValues(GeneralParameterNames.DOMAIN_PROFILE).get(0));
                } catch (URISyntaxException e) {
                    showError(TextFactory.getText(ErrorKey.DOMAIN_PROFILE_PARSE_ERROR));
                    view.scrollToTop();
                }

                if (domainProfileURI != null) {
                    for (Map.Entry<String, URI> idEntry : domainProfileIdMap.entrySet()) {
                        if (idEntry.getValue().equals(domainProfileURI)) {
                            view.getDomainProfilesComboBox().setValue(idEntry.getKey());
                            break;
                        }
                    }
                }
            }

            view.getAllDynamicFields().stream().filter(node ->
                                                           getController().getPackageState().getPackageMetadataValues(node.getId()) !=
                                                               null).forEach(node -> {
                if (node instanceof TextPropertyBox) {
                    if (node.getUserData() == null || !((String)node.getUserData()).equalsIgnoreCase("repeatable")) {
                        ((TextPropertyBox) node).getPropertyInput().setText(getController().getPackageState().getPackageMetadataValues(node.getId()).get(0));
                    }
                } else if (node instanceof DatePropertyBox) {
                    DateTime date = DateUtility.parseDateString(getController().getPackageState().getPackageMetadataValues(node.getId()).get(0));
                    if (date != null) {
                        ((DatePropertyBox) node).getPropertyInput().setValue(LocalDate.of(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
                    }
                } else if (node instanceof VBox) {
                    getController().getPackageState().getPackageMetadataValues(node.getId()).stream().filter(value -> !Util.isEmptyOrNull(value)).forEach(value -> ((VBox) node).getChildren().add(new RemovableLabel(value, (VBox) node)));
                }
            });
        }
    }

    private void bind() {

        List<DomainProfile> domainProfileList = domainProfileStore.getPrimaryDomainProfiles();
        List<String> domainProfileLabels = new ArrayList<>();
        domainProfileIdMap = new HashMap<>();
        for (DomainProfile domainProfile : domainProfileList){
            domainProfileLabels.add(domainProfile.getLabel());
            domainProfileIdMap.put(domainProfile.getLabel(), domainProfile.getIdentifier());
        }
        view.loadDomainProfileNames(domainProfileLabels);

        if (!view.isFormAlreadyDrawn()) {
            view.setupRequiredFields(packageMetadataService.getRequiredPackageMetadata());
            view.setupRecommendedFields(packageMetadataService.getRecommendedPackageMetadata());
            view.setupOptionalFields(packageMetadataService.getOptionalPackageMetadata());
        }

        view.getSaveButton().setOnAction(event -> {
            updatePackageState();
            try{
               getController().savePackageStateFile();
            } catch (IOException | RDFTransformException e){
                showError(TextFactory.getText(ErrorKey.IO_CREATE_ERROR));
                view.scrollToTop();
            }
        });

    }

    @Override
    public void onContinuePressed() {
        updatePackageState();

        if(!validateRequiredFields()){
            showError(TextFactory.getText(ErrorKey.MISSING_REQUIRED_FIELDS));;
            view.scrollToTop();
        } else if(!view.areAllFieldsValid()) {
            showError(TextFactory.getText(ErrorKey.SOME_FIELDS_INVALID));
            view.scrollToTop();
        } else  {
             if (Platform.isFxApplicationThread()) {
                try {
                    getController().savePackageStateFile();
                } catch (IOException | RDFTransformException e) {
                    showError(TextFactory.getText(ErrorKey.IO_CREATE_ERROR));
                    view.scrollToTop();
                }
            }

            clearError();
            super.onContinuePressed();
        }
    }

    @Override
    public void onBackPressed() {
        if (areAllFieldsEmpty()) {
            clearError();
            controller.goToPreviousPage();
            return;
        }

        updatePackageState();
        super.onBackPressed();
    }

    /**
     * helper method that updates the PackageState in the controller with the values in the form.
     */
    private void updatePackageState() {

        // Let's first fetch the static fields' values and update the PackageState with them.
        if (view.getPackageNameField().isValid().getValue() && !Util.isEmptyOrNull(view.getPackageNameField().getValueAsString())) {
            getController().getPackageState().setPackageName((String) view.getPackageNameField().getValue());
        }

        // Clear the package metadata list and reset the values based on the current state of form
        getController().getPackageState().setPackageMetadataList(new LinkedHashMap<>());

        // Clear the domain profile list and reset the values on the current state of the form
        getController().getPackageState().setDomainProfileIdList(new ArrayList<>());
        String domainProfileName = view.getDomainProfilesComboBox().getValue();
        if(domainProfileName != null && !domainProfileName.isEmpty()) {
            List<URI> domainProfileIdList = new ArrayList<>();
            domainProfileIdList.add(domainProfileIdMap.get(domainProfileName));
            getController().getPackageState().setDomainProfileIdList(domainProfileIdList);
            domainProfileIdList.forEach(profileId ->
                    getController().getPackageState().addPackageMetadata(
                            GeneralParameterNames.DOMAIN_PROFILE, profileId.toString()));
        }

        // Now let's go through the dynamic fields and update the package state.
        for (Node node : view.getAllDynamicFields()) {
            if (node instanceof PropertyBox) {
                PropertyBox propertyBox = (PropertyBox) node;
                if (propertyBox.isValid().getValue() && !Util.isEmptyOrNull(propertyBox.getValueAsString())) {
                    getController().getPackageState().addPackageMetadata(node.getId(), propertyBox.getValueAsString());
                }
            } else if (node instanceof VBox) {
                ((VBox) node).getChildren().stream().filter(removableLabel -> removableLabel instanceof RemovableLabel).forEach(removableLabel -> getController().getPackageState().addPackageMetadata(node.getId(), ((RemovableLabel) removableLabel).getLabel().getText()));
            }
        }
    }

    private boolean areAllFieldsEmpty() {
        // Let's first fetch the static fields' values and update the PackageState with them.
        if(view.getPackageNameField().getValue() != null && !((String)view.getPackageNameField().getValue()).isEmpty()) {
            return false;
        }

        // Now let's go through the dynamic fields and update the package state.
        for (Node node : view.getAllDynamicFields()) {
            if (node instanceof TextField) {
                if (((TextField) node).getText() != null && !((TextField) node).getText().isEmpty()) {
                    return false;
                }
            } else if (node instanceof DatePicker) {
                if (((DatePicker) node).getValue() != null && !((DatePicker)node).getValue().equals(LocalDate.now())) {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * Helper method that makes sure required fields are entered before letting the user move on to the next page.
     *
     * @return true or false based on validation
     */
    private boolean validateRequiredFields() {

        //first check the editable required fields
        for (PackageMetadata reqField : packageMetadataService.getRequiredPackageMetadata()) {
            if (reqField.isEditable() && getController().getPackageState().getPackageMetadataValues(reqField.getName()) == null) {
                return false;
            }
        }

        //now check other required fields
        return !(getController().getPackageState().getPackageName() == null ||
                     getController().getPackageState().getPackageName().isEmpty() ||
                     getController().getPackageState().getDomainProfileIdList() ==
                         null ||
                     getController().getPackageState().getDomainProfileIdList().isEmpty() ||
                     !getController().getPackageState().getPackageMetadataList().keySet().contains(GeneralParameterNames.DOMAIN_PROFILE));

    }

    @Override
    public void setPackageMetadataService(PackageMetadataService packageMetadataService) {
        this.packageMetadataService = packageMetadataService;
    }

    @Override
    public void setDomainProfileStore(DomainProfileStore domainProfileStore) {
        this.domainProfileStore = domainProfileStore;
    }

}
