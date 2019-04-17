package org.hisp.dhis.programstagefilter;
/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
@Transactional
public class DefaultProgramStageInstanceFilterService implements ProgramStageInstanceFilterService
{

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramStageInstanceFilterStore programStageInstanceFilterStore;

    private ProgramService programService;

    private ProgramStageService programStageService;

    private OrganisationUnitService organisationUnitService;

    private AclService aclService;

    private CurrentUserService currentUserService;

    @Autowired
    public void setProgramStageInstanceFilterStore( ProgramStageInstanceFilterStore programStageInstanceFilterStore )
    {
        this.programStageInstanceFilterStore = programStageInstanceFilterStore;
    }

    @Autowired
    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    @Autowired
    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    @Autowired
    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    @Autowired
    public void setAclService( AclService aclService )
    {
        this.aclService = aclService;
    }

    @Autowired
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // ProgramStageInstanceFilterService implementation
    // -------------------------------------------------------------------------

    @Override
    public long add( ProgramStageInstanceFilter programStageInstanceFilter )
    {
        List<String> errors = validate( programStageInstanceFilter );

        if ( !errors.isEmpty() )
        {
            throw new IllegalQueryException( errors.toString() );
        }
        programStageInstanceFilterStore.save( programStageInstanceFilter );
        return programStageInstanceFilter.getId();
    }

    @Override
    public void delete( ProgramStageInstanceFilter programStageInstanceFilter )
    {
        if ( !aclService.canDelete( currentUserService.getCurrentUser(), programStageInstanceFilter ) )
        {
            throw new DeleteAccessDeniedException( "You do not have the authority to delete the eventFilter: '" + programStageInstanceFilter.getUid() + "'" );

        }
        programStageInstanceFilterStore.delete( programStageInstanceFilter );
    }

    @Override
    public void update( ProgramStageInstanceFilter programStageInstanceFilter )
    {
        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), programStageInstanceFilter ) )
        {
            throw new UpdateAccessDeniedException( "You do not have the authority to update the eventFilter: '" + programStageInstanceFilter.getUid() + "'" );

        }

        List<String> errors = validate( programStageInstanceFilter );

        if ( !errors.isEmpty() )
        {
            throw new IllegalQueryException( errors.toString() );
        }
        programStageInstanceFilterStore.update( programStageInstanceFilter );
    }

    private List<String> validate( ProgramStageInstanceFilter programStageInstanceFilter )
    {
        List<String> errors = new ArrayList<>();

        if ( programStageInstanceFilter.getProgram() == null )
        {
            errors.add( "Program should be specified for event filters." );
        }
        else
        {
            Program pr = programService.getProgram( programStageInstanceFilter.getProgram() );

            if ( pr == null )
            {
                errors.add( "Program is specified but does not exist: " + programStageInstanceFilter.getProgram() );
            }
        }

        if ( programStageInstanceFilter.getProgramStage() != null )
        {
            ProgramStage ps = programStageService.getProgramStage( programStageInstanceFilter.getProgramStage() );
            if ( ps == null )
            {
                errors.add( "Program stage is specified but does not exist: " + programStageInstanceFilter.getProgramStage() );
            }
        }

        EventQueryCriteria eventQC = programStageInstanceFilter.getEventQueryCriteria();
        if ( eventQC != null )
        {
            if ( eventQC.getOrganisationUnit() != null )
            {
                OrganisationUnit ou = organisationUnitService.getOrganisationUnit( eventQC.getOrganisationUnit() );
                if ( ou == null )
                {
                    errors.add( "Org unit is specified but does not exist: " + eventQC.getOrganisationUnit() );
                }
            }
            if ( eventQC.getAssignedUserMode() != null && eventQC.getAssignedUsers() != null && !eventQC.getAssignedUsers().isEmpty()
                && !eventQC.getAssignedUserMode().equals( AssignedUserSelectionMode.PROVIDED ) )
            {
                errors.add( "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );
            }

            if ( eventQC.getEvents() != null && !eventQC.getEvents().isEmpty() && eventQC.getDataFilters() != null && !eventQC.getDataFilters().isEmpty() )
            {
                errors.add( "Event UIDs and filters can not be specified at the same time" );
            }
        }

        return errors;
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramStageInstanceFilter get( long id )
    {
        ProgramStageInstanceFilter psiFilter = programStageInstanceFilterStore.get( id );
        if ( !aclService.canRead( currentUserService.getCurrentUser(), psiFilter ) )
        {
            throw new ReadAccessDeniedException( "You do not have the authority to read the eventFilter with id: '" + id + "'" );

        }
        return psiFilter;
    }

    @Override
    public ProgramStageInstanceFilter get( String uid )
    {
        ProgramStageInstanceFilter psiFilter = programStageInstanceFilterStore.getByUid( uid );
        if ( !aclService.canRead( currentUserService.getCurrentUser(), psiFilter ) )
        {
            throw new ReadAccessDeniedException( "You do not have the authority to read the eventFilter: '" + uid + "'" );
        }
        return psiFilter;
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramStageInstanceFilter> getAll( String program )
    {
        List<ProgramStageInstanceFilter> psiFilters;
        if ( program != null )
        {
            psiFilters = programStageInstanceFilterStore.getByProgram( program );
        }
        else
        {
            psiFilters = programStageInstanceFilterStore.getAll();
        }

        List<ProgramStageInstanceFilter> filteredEventFilters = psiFilters.stream()
            .filter( psiFilter -> aclService.canRead( currentUserService.getCurrentUser(), psiFilter ) )
            .collect( Collectors.toList() );

        return filteredEventFilters;
    }

}