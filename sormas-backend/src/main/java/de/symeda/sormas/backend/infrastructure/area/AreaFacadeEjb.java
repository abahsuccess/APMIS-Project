package de.symeda.sormas.backend.infrastructure.area;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.vladmihalcea.hibernate.type.util.SQLExtractor;

import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.data.CampaignAggregateDataDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.infrastructure.area.AreaCriteria;
import de.symeda.sormas.api.infrastructure.area.AreaDto;
import de.symeda.sormas.api.infrastructure.area.AreaFacade;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.utils.SortProperty;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.backend.campaign.Campaign;
import de.symeda.sormas.backend.feature.FeatureConfigurationFacadeEjb.FeatureConfigurationFacadeEjbLocal;
import de.symeda.sormas.backend.infrastructure.AbstractInfrastructureEjb;
import de.symeda.sormas.backend.infrastructure.PopulationData;
import de.symeda.sormas.backend.infrastructure.community.Community;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.region.Region;
import de.symeda.sormas.backend.infrastructure.region.RegionService;
import de.symeda.sormas.backend.util.DtoHelper;
import de.symeda.sormas.backend.util.ModelConstants;
import de.symeda.sormas.backend.util.QueryHelper;

@Stateless(name = "AreaFacade")
public class AreaFacadeEjb extends AbstractInfrastructureEjb<Area, AreaService> implements AreaFacade {

	@EJB
	private RegionService regionService;
	
	@EJB
	private AreaService areaService;
	
	@PersistenceContext(unitName = ModelConstants.PERSISTENCE_UNIT_NAME)
	private EntityManager em;

	public AreaFacadeEjb() {
	}

	@Inject
	protected AreaFacadeEjb(AreaService service, FeatureConfigurationFacadeEjbLocal featureConfiguration) {
		super(service, featureConfiguration);
	}

	@Override
	public List<AreaReferenceDto> getAllActiveAndSelectedAsReference(String campaignUuid) {
		
		String selectBuilder = "select distinct a.uuid, a.\"name\", a.externalid\r\n"
				+ "from areas a\r\n"
				+ "inner join region r on a.id = r.area_id\r\n"
				+ "inner join populationdata p on r.id = p.region_id\r\n"
				+ "inner join campaigns c on p.campaign_id = c.id\r\n"
				+ "where c.uuid = '"+campaignUuid+"' and p.selected = true and a.archived = false;";
		
		Query seriesDataQuery = em.createNativeQuery(selectBuilder);
		
		
		@SuppressWarnings("unchecked")
		List<Object[]> resultList = seriesDataQuery.getResultList(); 
		
		List<AreaReferenceDto> resultData = new ArrayList<>();
		resultData.addAll(resultList.stream()
				.map((result) -> new AreaReferenceDto(
						(String) result[0], (String) result[1], ((BigInteger) result[2]).longValue()
						
						)).collect(Collectors.toList()));
						
		
	
		return resultData;
		//
		
		//return service.getAllActive(Area.NAME, true).stream().map(AreaFacadeEjb::toReferenceDto).collect(Collectors.toList());
	}
	
	@Override
	public List<AreaReferenceDto> getAllActiveAsReference() {
		return service.getAllActive(Area.NAME, true).stream().map(AreaFacadeEjb::toReferenceDto).collect(Collectors.toList());
	}

	@Override
	public AreaDto getByUuid(String uuid) {
		return toDto(service.getByUuid(uuid));
	}

	@Override
	public List<AreaDto> getIndexList(AreaCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Area> cq = cb.createQuery(Area.class);
		Root<Area> areaRoot = cq.from(Area.class);

		Predicate filter = service.buildCriteriaFilter(criteria, cb, areaRoot);
		if (filter != null) {
			cq.where(filter);
		}

		if (sortProperties != null && sortProperties.size() > 0) {
			List<Order> order = new ArrayList<>(sortProperties.size());
			for (SortProperty sortProperty : sortProperties) {
				Expression<?> expression;
				switch (sortProperty.propertyName) {
				case Area.NAME:
				case Area.EXTERNAL_ID:
					expression = areaRoot.get(sortProperty.propertyName);
					break;
				default:
					throw new IllegalArgumentException(sortProperty.propertyName);
				}
				order.add(sortProperty.ascending ? cb.asc(expression) : cb.desc(expression));
			}
			cq.orderBy(order);
		} else {
			cq.orderBy(cb.asc(areaRoot.get(Area.NAME)));
		}

		cq.select(areaRoot);

		return QueryHelper.getResultList(em, cq, first, max, this::toDto);
	}

	@Override
	public long count(AreaCriteria criteria) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<Area> areaRoot = cq.from(Area.class);

		Predicate filter = service.buildCriteriaFilter(criteria, cb, areaRoot);
		if (filter != null) {
			cq.where(filter);
		}

		cq.select(cb.count(areaRoot));
		return em.createQuery(cq).getSingleResult();
	}

	@Override
	public AreaDto save(@Valid AreaDto dto) {
		return save(dto, false);
	}

	@Override
	public AreaDto save(@Valid AreaDto dto, boolean allowMerge) {
		checkInfraDataLocked();
		Area area = service.getByUuid(dto.getUuid());

		if (area == null) {
			List<Area> duplicates = service.getByName(dto.getName(), true);
			if (!duplicates.isEmpty()) {
				if (allowMerge) {
					area = duplicates.get(0);
					AreaDto dtoToMerge = getByUuid(area.getUuid());
					dto = DtoHelper.copyDtoValues(dtoToMerge, dto, true);
				} else {
					throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.importAreaAlreadyExists));
				}
			}
		}

		area = fromDto(dto, area, true);
		service.ensurePersisted(area);
		return toDto(area);
	}

	@Override
	public boolean isUsedInOtherInfrastructureData(Collection<String> areaUuids) {
		return service.isUsedInInfrastructureData(areaUuids, Region.AREA, Region.class);
	}

	@Override
	public List<AreaReferenceDto> getByName(String name, boolean includeArchived) {
		return service.getByName(name, includeArchived).stream().map(AreaFacadeEjb::toReferenceDto).collect(Collectors.toList());
	}
	
	@Override
	public List<AreaReferenceDto> getByExternalID(Long ext_id, boolean includeArchived) {
		return service.getByExternalID(ext_id, includeArchived).stream().map(AreaFacadeEjb::toReferenceDto).collect(Collectors.toList());
	}

	@Override
	public List<AreaDto> getAllAfter(Date date) {
		return service.getAll((cb, root) -> service.createChangeDateFilter(cb, root, date)).stream().map(this::toDto).collect(Collectors.toList());
	}

	@Override
	public List<AreaDto> getByUuids(List<String> uuids) {
		return service.getByUuids(uuids).stream().map(this::toDto).collect(Collectors.toList());
	}

	@Override
	public List<String> getAllUuids() {
		return service.getAllUuids();
	}

	public Area fromDto(@NotNull AreaDto source, Area target, boolean checkChangeDate) {
		target = DtoHelper.fillOrBuildEntity(source, target, Area::new, checkChangeDate);

		target.setName(source.getName());
		target.setExternalId(source.getExternalId());
		target.setArchived(source.isArchived());

		return target;
	}

	@Override
	public List<AreaReferenceDto> getByExternalId(Long externalId, boolean includeArchivedEntities) {

		return service.getByExternalId(externalId, includeArchivedEntities).stream().map(AreaFacadeEjb::toReferenceDto).collect(Collectors.toList());
	}

	public AreaDto toDto(Area source) {
		if (source == null) {
			return null;
		}
		AreaDto target = new AreaDto();
		DtoHelper.fillDto(target, source);

		target.setName(source.getName());
		target.setExternalId(source.getExternalId());
		target.setArchived(source.isArchived());

		return target;
	}

	public static AreaReferenceDto toReferenceDto(Area entity) {
		if (entity == null) {
			return null;
		}
		return new AreaReferenceDto(entity.getUuid(), entity.toString());
	}
	
	public static AreaReferenceDto toReferenceDtox(Area entity) {
		if (entity == null) {
			return null;
		}
		return new AreaReferenceDto(entity.getUuid(), entity.toString(), entity.getExternalId());
	}

	@Override
	public List<AreaReferenceDto> getReferencesByName(String name, boolean includeArchived) {
		return getByName(name, includeArchived);
	}

	@LocalBean
	@Stateless
	public static class AreaFacadeEjbLocal extends AreaFacadeEjb {

		public AreaFacadeEjbLocal() {
		}

		@Inject
		protected AreaFacadeEjbLocal(AreaService service, FeatureConfigurationFacadeEjbLocal featureConfiguration) {
			super(service, featureConfiguration);
		}
	}


	public static Set<AreaReferenceDto> toReferenceDto(HashSet<Area> areas) {
		Set<AreaReferenceDto> dtos = new HashSet<AreaReferenceDto>();
		for(Area area : areas) {	
			AreaReferenceDto areaDto = new AreaReferenceDto(area.getUuid(), area.toString(), area.getExternalId());	
			dtos.add(areaDto);
		}
		
		return dtos;
	}
	
	@Override
	public AreaReferenceDto getAreaReferenceByUuid(String uuid) {
		return toReferenceDto(areaService.getByUuid(uuid));
	}
	
	@Override
	public List<AreaDto> getAllActiveAsReferenceAndPopulation() {
		
		
		String queryStringBuilder = "select a.\"name\", sum(p.population), a.id, a.uuid as mdis, a.externalid as exter  from areas a \n"
				+ "left outer join region r on r.area_id = a.id\n"
				+ "left outer join populationdata p on r.id = p.region_id\r\n"
				+ "where a.archived = false and p.agegroup = 'AGE_0_4'\n"
				+ "group by a.\"name\", a.id, a.uuid ";
		
		
		Query seriesDataQuery = em.createNativeQuery(queryStringBuilder);
		
		List<AreaDto> resultData = new ArrayList<>();
		
		
		@SuppressWarnings("unchecked")
		List<Object[]> resultList = seriesDataQuery.getResultList(); 
		
		System.out.println("starting....");
		
		resultData.addAll(resultList.stream()
				.map((result) -> new AreaDto((String) result[0].toString(), ((BigInteger) result[1]).longValue(), ((BigInteger) result[2]).longValue(), (String) result[3].toString(), ((BigInteger) result[4]).longValue())).collect(Collectors.toList()));
		
		System.out.println("ending...." +resultData.size());
	
	
	//System.out.println("resultData - "+ resultData.toString()); //SQLExtractor.from(seriesDataQuery));
	return resultData;
	}

	@Override
	public List<AreaDto> getAllActiveAsReferenceAndPopulation(CampaignDto campaignDt) {
		List<AreaDto> resultData = new ArrayList<>();
			String queryStringBuilder = "select a.\"name\", sum(p.population), a.id, a.uuid as mdis, a.externalid as exter  from areas a \n"
					+ "left outer join region r on r.area_id = a.id\n"
					+ "left outer join populationdata p on r.id = p.region_id\n"
					+ "left outer join campaigns ca on p.campaign_id = ca.id \n"
					+ "where a.archived = false and p.agegroup = 'AGE_0_4' and ca.uuid = '" + campaignDt.getUuid()
					+ "'\n" + "group by a.\"name\", a.id, a.uuid ";

			
			System.out.println(queryStringBuilder + "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
			Query seriesDataQuery = em.createNativeQuery(queryStringBuilder);

			@SuppressWarnings("unchecked")
			List<Object[]> resultList = seriesDataQuery.getResultList();
			resultData.addAll(resultList.stream()
					.map((result) -> new AreaDto((String) result[0].toString(), ((BigInteger) result[1]).longValue(),
							((BigInteger) result[2]).longValue(), (String) result[3].toString(), ((BigInteger) result[4]).longValue()))
					.collect(Collectors.toList()));
		
		return resultData;

	}
}
