/*******************************************************************************
 * Copyright (c) 2013-2015 LAAS-CNRS (www.laas.fr)
 * 7 Colonel Roche 31077 Toulouse - France
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Monteil (Project co-founder) - Management and initial specification,
 *         conception and documentation.
 *     Mahdi Ben Alaya (Project co-founder) - Management and initial specification,
 *         conception, implementation, test and documentation.
 *     Christophe Chassot - Management and initial specification.
 *     Khalil Drira - Management and initial specification.
 *     Yassine Banouar - Initial specification, conception, implementation, test
 *         and documentation.
 *     Guillaume Garzone - Conception, implementation, test and documentation.
 *     Francois Aissaoui - Conception, implementation, test and documentation.
 ******************************************************************************/
package org.eclipse.om2m.core.controller;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;

import org.eclipse.om2m.commons.resource.AnnounceTo;
import org.eclipse.om2m.commons.resource.Application;
import org.eclipse.om2m.commons.resource.ErrorInfo;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.commons.utils.DateConverter;
import org.eclipse.om2m.commons.utils.XmlMapper;
import org.eclipse.om2m.core.announcer.Announcer;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.dao.DAOFactory;
import org.eclipse.om2m.core.dao.DBAccess;
import org.eclipse.om2m.core.notifier.Notifier;

/**
 * Implements Create, Retrieve, Update, Delete and Execute methods to handle
 * generic REST request for {@link Application} resource.
 *
 * @author <ul>
 *         <li>Yassine Banouar < ybanouar@laas.fr > < yassine.banouar@gmail.com ></li>
 *         <li>Mahdi Ben Alaya < ben.alaya@laas.fr > < benalaya.mahdi@gmail.com ></li>
 *         </ul>
 */

public class ApplicationController extends Controller {

    /**
     * Creates {@link Application} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doCreate (RequestIndication requestIndication)  {

        // containersReference:             (createReq NP) (response M)
        // groupsReference:                 (createReq NP) (response M)
        // accessRightsReference:           (createReq NP) (response M)
        // subscriptionsReference:          (createReq NP) (response M)
        // notificationChannelsReference:   (createReq NP) (response M)
        // appId:                           (createReq O)  (response M)
        // expirationTime:                  (createReq O)  (response M*)
        // accessRightID:                   (createReq O)  (response O)
        // searchStrings:                   (createReq O)  (response M)
        // creationTime:                    (createReq NP) (response M)
        // lastModifiedTime:                (createReq NP) (response M)
        // announceTo:                      (createReq O)  (response M*)
        // aPoC:                            (createReq O)  (response O)
        // aPoCPaths:                       (createReq O)  (response O)
        // locRequester:                    (createReq O)  (response O)

        ResponseConfirm errorResponse = new ResponseConfirm();
        
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        
        String accessRightID = getAccessRightId(requestIndication.getTargetID(), em);

        // Check Parent Existence
        if (accessRightID == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+" does not exist"));
        }
        // Check AccessRight
        errorResponse = checkAccessRight(accessRightID,requestIndication.getRequestingEntity(), Constants.AR_CREATE);
        if (errorResponse != null) {
        	em.close();
            return errorResponse;
        }
        // Check Resource Representation
        if (requestIndication.getRepresentation() == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Resource Representation is EMPTY"));
        }
        
        // Checks on attributes
        Application application = null ; 
        try{
        	application = (Application) XmlMapper.getInstance().xmlToObject(requestIndication.getRepresentation());        	
        } catch (ClassCastException e){
        	em.close();
        	LOGGER.debug("ClassCastException : Incorrect resource type in JAXB unmarshalling.",e);
        	return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource type"));
        }
        if (application == null){
        	em.close();
        	return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource representation syntax")) ;         	
        }
        // Check ID Conformity
        if (application.getAppId() != null && !application.getAppId().matches(Constants.ID_REGEXPR)){
        	em.close();
        	return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"AppId should match the following regexpr: " + Constants.ID_REGEXPR));
        }
        // Check the AppId uniqueness
        if (application.getAppId() != null && DAOFactory.getApplicationDAO().find(requestIndication.getTargetID()+"/"+application.getAppId(), em) != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_CONFLICT,"ApplicationId Conflit"));
        }
        if (application.getAppId() == null || application.getAppId().isEmpty()) {
            application.setAppId(generateId("APP_",""));
        } 
        // Check ExpirationTime
        if (application.getExpirationTime() != null && !checkExpirationTime(application.getExpirationTime())) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Expiration Time CREATE is Out of Date"));
        }
        // Containers Reference Must be NP
        if (application.getContainersReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST," Containers Reference is Not Permitted"));
        }
        // Groups Reference Must be NP
        if (application.getGroupsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Groups Reference is Not Permitted"));
        }
        // AccessRightsReference Must be NP
        if (application.getAccessRightsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"AccessRights Reference is Not Permitted"));
        }
        // SubscriptionsReference Must be NP
        if (application.getSubscriptionsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"SubscriptionsReference is Not Permitted"));
        }
        // NotificationChannelsReference Must be NP
        if (application.getNotificationChannelsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"NotificationChannelsReference is Not Permitted"));
        }
        // CreationTime Must be NP
        if (application.getCreationTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Creation Time is Not Permitted"));
        }
        // LastModifiedTime Must be NP
        if (application.getLastModifiedTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Last Modified Time is Not Permitted"));
        }
        // Storage
        // Set URI
        application.setUri(requestIndication.getTargetID()+ "/" +application.getAppId());
        // Set Expiration Time if it's null
        if (application.getExpirationTime() == null) {
            //Expiration default value
            application.setExpirationTime(getNewExpirationTime(Constants.EXPIRATION_TIME));
        }
//         Set AccessRightID from the Parent if it's null or nonexistent
        if (DAOFactory.getAccessRightDAO().find(application.getAccessRightID(), em) == null) {
            application.setAccessRightID(accessRightID);
        }
        if (application.getAPoCPaths() != null) {
            for (int i = 0; i<application.getAPoCPaths().getAPoCPath().size(); i++) {
                if (DAOFactory.getAccessRightDAO().find(application.getAPoCPaths().getAPoCPath().get(i).getAccessRightID(), em) == null) {
                    application.getAPoCPaths().getAPoCPath().get(i).setAccessRightID(application.getAccessRightID());
                }
            }
        }
        // Set searchString if it is null
        if (application.getSearchStrings() == null) {
            application.setSearchStrings(generateSearchStrings(application.getClass().getSimpleName(), application.getAppId()));
        }
        // Set announceTo if it is null
        if (application.getAnnounceTo() == null) {
            AnnounceTo announceTo = new AnnounceTo();
            announceTo.setActivated(false);
            announceTo.setGlobal(false);
            application.setAnnounceTo(announceTo);
        }
        // Set References
        application.setContainersReference(application.getUri()+Refs.CONTAINERS_REF);
        application.setGroupsReference(application.getUri()+Refs.GROUPS_REF);
        application.setAccessRightsReference(application.getUri()+Refs.ACCESSRIGHTS_REF);
        application.setSubscriptionsReference(application.getUri()+Refs.SUBSCRIPTIONS_REF);
        application.setNotificationChannelsReference(application.getUri()+Refs.NOTIFICATIONCHANNELS_REF);
        // Set CreationTime
        application.setCreationTime(DateConverter.toXMLGregorianCalendar(new Date()).toString());
        // Set LastModifiedTime
        application.setLastModifiedTime(DateConverter.toXMLGregorianCalendar(new Date()).toString());

        // Announcement
        if (application.getAnnounceTo().isActivated()) {
            application.setAnnounceTo(new Announcer().announce(application.getAnnounceTo(), application.getUri(), application.getSearchStrings(), requestIndication.getRequestingEntity()));
        }

        // Notification
        Notifier.notify(StatusCode.STATUS_CREATED, application);

        // Store application
        try{
        	DAOFactory.getApplicationDAO().create(application, em);
        	em.getTransaction().commit();
        	em.close();
        } catch (RollbackException e){
        	em.close();
        	LOGGER.debug("ApplicationId Conflict",e);
        	return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_CONFLICT,"ApplicationId Conflit"));
        }
        // Response
        return new ResponseConfirm(StatusCode.STATUS_CREATED, application);
    }

    /**
     * Retrieves {@link Application} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doRetrieve (RequestIndication requestIndication) {

        // containersReference:             (response M)
        // groupsReference:                 (response M)
        // accessRightsReference:           (response M)
        // subscriptionsReference:          (response M)
        // notificationChannelsReference:   (response M)
        // appId:                           (response M)
        // expirationTime:                  (response M*)
        // accessRightID:                   (response O)
        // searchStrings:                   (response M)
        // creationTime:                    (response M)
        // lastModifiedTime:                (response M)
        // announceTo:                      (response M*)
        // aPoC:                            (response O)
        // aPoCPaths:                       (response O)
        // locRequester:                    (response O)

        ResponseConfirm errorResponse = new ResponseConfirm();
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Application application = DAOFactory.getApplicationDAO().find(requestIndication.getTargetID(), em);
        em.close();

        // Check if the resource exists in DataBase or Not
        if (application == null) {
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+" does not exist in DataBase"));
        }
        // Check AccessRight
        errorResponse = checkAccessRight(application.getAccessRightID(), requestIndication.getRequestingEntity(), Constants.AR_READ);
        if (errorResponse != null) {
            return errorResponse;
        }
        
		application.setContainersReference(application.getUri() + Refs.CONTAINERS_REF);
		application.setGroupsReference(application.getUri() + Refs.GROUPS_REF);
		application.setAccessRightsReference(application.getUri() + Refs.ACCESSRIGHTS_REF);
		application.setSubscriptionsReference(application.getUri() + Refs.SUBSCRIPTIONS_REF);
		application.setNotificationChannelsReference(application.getUri() + Refs.NOTIFICATIONCHANNELS_REF);
		
        // Response
        return new ResponseConfirm(StatusCode.STATUS_OK, application);
    }

    /**
     * Updates {@link Application} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doUpdate (RequestIndication requestIndication) {

        // containersReference:             (updateReq NP) (response M)
        // groupsReference:                 (updateReq NP) (response M)
        // accessRightsReference:           (updateReq NP) (response M)
        // subscriptionsReference:          (updateReq NP) (response M)
        // notificationChannelsReference:   (updateReq NP) (response M)
        // appId:                           (updateReq NP) (response M)
        // expirationTime:                  (updateReq O)  (response M*)
        // accessRightID:                   (updateReq O)  (response O)
        // searchStrings:                   (updateReq O)  (response M)
        // creationTime:                    (updateReq NP) (response M)
        // lastModifiedTime:                (updateReq NP) (response M)
        // announceTo:                      (updateReq O)  (response M*)
        // aPoC:                            (updateReq O)  (response O)
        // aPoCPaths:                       (updateReq O)  (response O)
        // locRequester:                    (updateReq O)  (response O)

        ResponseConfirm errorResponse = new ResponseConfirm();
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Application application = DAOFactory.getApplicationDAO().find(requestIndication.getTargetID(), em);

        // Check Existence
        if (application == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+" does not exist in DataBase"));
        }
        // Check AccessRight
        errorResponse = checkAccessRight(application.getAccessRightID(), requestIndication.getRequestingEntity(), Constants.AR_WRITE);
        if (errorResponse != null) {
        	em.close();
            return errorResponse;
        }
        // Check Resource Representation
        if (requestIndication.getRepresentation() == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Resource Representation is EMPTY"));
        }
        // Checks on attributes 
        Application applicationNew = null ;
        try {
        	applicationNew = (Application) XmlMapper.getInstance().xmlToObject(requestIndication.getRepresentation());
        } catch(ClassCastException e){
        	em.close();
        	LOGGER.debug("ClassCastException : Incorrect resource type in JAXB unmarshalling.",e);
        	return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource type"));
        }
        if (applicationNew == null){
        	em.close();
        	return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource representation syntax"));
        }
        //The Update of the AppId is NP
        if (applicationNew.getAppId() != null) {
        	if (!applicationNew.getAppId().equals(application.getAppId())){      
        		em.close();
        		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"AppId UPDATE is Not Permitted"));
        	}
        }
        // Check ExpirationTime
        if (applicationNew.getExpirationTime() != null && !checkExpirationTime(applicationNew.getExpirationTime())) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Expiration Time UPDATE is Out of Date"));
        }
        // Containers Reference Must be NP
        if (applicationNew.getContainersReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Containers Reference UPDATE is Not Permitted"));
        }
        // Groups Reference Must be NP
        if (applicationNew.getGroupsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Groups Reference UPDATE is Not Permitted"));
        }
        // AccessRightsReference Must be NP
        if (applicationNew.getAccessRightsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"AccessRights Reference UPDATE is Not Permitted"));
        }
        // SubscriptionsReference Must be NP
        if (applicationNew.getSubscriptionsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"SubscriptionsReference UPDATE is Not Permitted"));
        }
        // NotificationChannelsReference Must be NP
        if (applicationNew.getNotificationChannelsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"NotificationChannelsReference UPDATE is Not Permitted"));
        }
        // CreationTime Must be NP
        if (applicationNew.getCreationTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Creation Time UPDATE is Not Permitted"));
        }
        // LastModifiedTime Must be NP
        if (applicationNew.getLastModifiedTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Last Modified Time UPDATE is Not Permitted"));
        }
        // Storage
        // Set accessRightID if it exists
        if (DAOFactory.getAccessRightDAO().find(applicationNew.getAccessRightID(), em) != null) {
            application.setAccessRightID(applicationNew.getAccessRightID());
        }
        // Set Expiration Time (could be null)
        if (applicationNew.getExpirationTime() != null) {
            application.setExpirationTime(applicationNew.getExpirationTime());
        }
        // Set SearchStrings
        if (applicationNew.getSearchStrings() != null) {
            application.setSearchStrings(applicationNew.getSearchStrings());
        }
        // Set AnnounceTo
        if (applicationNew.getAnnounceTo() != null) {
            application.setAnnounceTo(applicationNew.getAnnounceTo());
        }
        // Set aPoc
        if (applicationNew.getAPoC() != null) {
            application.setAPoC(applicationNew.getAPoC());
        }
        // Set aPocPaths
        if (applicationNew.getAPoCPaths() != null) {
            application.setAPoCPaths(applicationNew.getAPoCPaths());
        }
        // Set locRequestor
        if (applicationNew.getLocRequestor() != null) {
            application.setLocRequestor(applicationNew.getLocRequestor());
        }
        //Set LastModifiedTime
        application.setLastModifiedTime(DateConverter.toXMLGregorianCalendar(new Date()).toString());

        // Notify the subscribers
        Notifier.notify(StatusCode.STATUS_OK, application);

        // Store updates
        DAOFactory.getApplicationDAO().update(application, em);

        em.getTransaction().commit();
        em.close();
        
		application.setContainersReference(application.getUri() + Refs.CONTAINERS_REF);
		application.setGroupsReference(application.getUri() + Refs.GROUPS_REF);
		application.setAccessRightsReference(application.getUri() + Refs.ACCESSRIGHTS_REF);
		application.setSubscriptionsReference(application.getUri() + Refs.SUBSCRIPTIONS_REF);
		application.setNotificationChannelsReference(application.getUri() + Refs.NOTIFICATIONCHANNELS_REF);
        
        // Response
        return new ResponseConfirm(StatusCode.STATUS_OK, application);
    }

    /**
     * Deletes {@link Application} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doDelete (RequestIndication requestIndication) {

        ResponseConfirm errorResponse = new ResponseConfirm();
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Application application = DAOFactory.getApplicationDAO().find(requestIndication.getTargetID(), em);

        // Check Resource Existence
        if (application == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+" does not exist"));
        }
        // Check AccessRight
        errorResponse = checkAccessRight(application.getAccessRightID(), requestIndication.getRequestingEntity(), Constants.AR_DELETE);
        if (errorResponse != null) {
        	em.close();
            return errorResponse;
        }

        // De-announcement
        if (application.getAnnounceTo().isActivated()) {
            new Announcer().deAnnounce(application.getAnnounceTo(), application.getUri(), requestIndication.getRequestingEntity());
        }

        // Notify the subscribers
        Notifier.notify(StatusCode.STATUS_DELETED, application);

        // Delete
        DAOFactory.getApplicationDAO().delete(application, em);
        em.getTransaction().commit();
        em.close();
        // Response
        return new ResponseConfirm(StatusCode.STATUS_OK);
    }

    /**
     * Executes {@link Application} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doExecute (RequestIndication requestIndication) {

        // Response
        return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED,requestIndication.getMethod()+" Method is not implmented"));
    }
}
