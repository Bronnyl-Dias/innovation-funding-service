package org.innovateuk.ifs.interview.mapper;

import org.innovateuk.ifs.application.mapper.ApplicationMapper;
import org.innovateuk.ifs.assessment.domain.Assessment;
import org.innovateuk.ifs.commons.mapper.BaseMapper;
import org.innovateuk.ifs.commons.mapper.GlobalMapperConfig;
import org.innovateuk.ifs.competition.mapper.CompetitionMapper;
import org.innovateuk.ifs.interview.domain.Interview;
import org.innovateuk.ifs.interview.resource.InterviewResource;
import org.innovateuk.ifs.user.mapper.ProcessRoleMapper;
import org.innovateuk.ifs.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueMappingStrategy;

/**
 * Maps between domain and resource DTO for {@link Interview}.
 */
@Mapper(
        config = GlobalMapperConfig.class,
        uses = {
                ProcessRoleMapper.class,
                ApplicationMapper.class,
                UserMapper.class,
                CompetitionMapper.class
        },
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL
)
public abstract class InterviewMapper extends BaseMapper<Interview, InterviewResource, Long> {

    @Mappings({
            @Mapping(source = "processEvent", target = "event"),
            @Mapping(source = "participant", target = "processRole"),
            @Mapping(source = "target", target = "application"),
            @Mapping(source = "target.name", target = "applicationName"),
            @Mapping(source = "target.competition", target = "competition"),
            @Mapping(source = "processState", target = "interviewState")
    })
    @Override
    public abstract InterviewResource mapToResource(Interview domain);

    @Mappings({
            @Mapping(target = "processEvent", source = "event"),
            @Mapping(target = "participant", source = "processRole"),
            @Mapping(target = "target", source = "application"),
            @Mapping(target = "processState", source = "interviewState", ignore = true),
            @Mapping(target = "lastModified", ignore = true)
    })
    @Override
    public abstract Interview mapToDomain(InterviewResource resource);

    public Long mapAssessmentToId(Assessment object) {
        if (object == null) {
            return null;
        }
        return object.getId();
    }
}
