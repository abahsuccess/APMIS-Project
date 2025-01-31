package com.cinoteck.application.views.configurations;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import com.cinoteck.application.UserProvider;
import com.flowingcode.vaadin.addons.gridexporter.GridExporter;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictCriteria;
import de.symeda.sormas.api.infrastructure.district.DistrictIndexDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.utils.SortProperty;

@PageTitle("Districts")
@Route(value = "districts", layout = ConfigurationsView.class)
public class DistrictView extends VerticalLayout {

	private static final long serialVersionUID = 1370022184569877189L;

	DistrictCriteria criteria;

	DistrictDataProvider districtDataProvider = new DistrictDataProvider();

	ConfigurableFilterDataProvider<DistrictIndexDto, Void, DistrictCriteria> filteredDataProvider;

	Grid<DistrictIndexDto> grid = new Grid<>(DistrictIndexDto.class, false);

	ComboBox<AreaReferenceDto> regionFilter = new ComboBox<>("Region");

	ComboBox<RegionReferenceDto> provinceFilter = new ComboBox<>("Province");

	TextField searchField = new TextField();

	Button resetFilters = new Button("Reset Filters");
	
	ComboBox<String> riskFilter = new ComboBox<>("Risk");
	ComboBox<EntityRelevanceStatus> relevanceStatusFilter = new ComboBox<>("Relevance Status");

	UserProvider currentUser = new UserProvider();

	@SuppressWarnings("deprecation")
	public DistrictView() {

		this.criteria = new DistrictCriteria();
		setSpacing(false);
		setHeightFull();

		grid.setSelectionMode(SelectionMode.SINGLE);
		grid.setMultiSort(true, MultiSortPriority.APPEND);
		grid.setSizeFull();
		grid.setColumnReorderingAllowed(true);
		grid.addColumn(DistrictIndexDto::getAreaname).setHeader("Region").setSortable(true).setResizable(true);
		grid.addColumn(DistrictIndexDto::getAreaexternalId).setHeader("Rcode").setResizable(true).setSortable(true);
		grid.addColumn(DistrictIndexDto::getRegion).setHeader("Province").setSortable(true).setResizable(true);
		grid.addColumn(DistrictIndexDto::getRegionexternalId).setHeader("PCode").setResizable(true).setSortable(true);
		grid.addColumn(DistrictIndexDto::getName).setHeader("District").setSortable(true).setResizable(true);
		grid.addColumn(DistrictIndexDto::getExternalId).setHeader("DCode").setResizable(true).setSortable(true);

		grid.setVisible(true);

		filteredDataProvider = districtDataProvider.withConfigurableFilter();

		grid.setDataProvider(filteredDataProvider);
		addFiltersLayout();
		add(grid);
	}

	public Component addFiltersLayout() {

		HorizontalLayout layout = new HorizontalLayout();
		layout.setPadding(false);
		layout.setVisible(false);
		layout.setAlignItems(Alignment.END);

		HorizontalLayout vlayout = new HorizontalLayout();
		vlayout.setPadding(false);

		vlayout.setAlignItems(Alignment.END);

		Button displayFilters = new Button("Show Filters", new Icon(VaadinIcon.SLIDERS));
		displayFilters.getStyle().set("margin-left", "1em");
		displayFilters.addClickListener(e -> {
			if (layout.isVisible() == false) {
				layout.setVisible(true);
				displayFilters.setText("Hide Filters");
			} else {
				layout.setVisible(false);
				displayFilters.setText("Show Filters");
			}
		});

		layout.setPadding(false);

		regionFilter.setPlaceholder("All Regions");
		regionFilter.setItems(FacadeProvider.getAreaFacade().getAllActiveAsReference());
		if (currentUser.getUser().getArea() != null) {
			regionFilter.setValue(currentUser.getUser().getArea());
			filteredDataProvider.setFilter(criteria.area(currentUser.getUser().getArea()));
			provinceFilter.setItems(
					FacadeProvider.getRegionFacade().getAllActiveByArea(currentUser.getUser().getArea().getUuid()));
			regionFilter.setEnabled(false);
		}

		layout.add(searchField);

		layout.add(regionFilter);

		provinceFilter.setPlaceholder("All Provinces");
		if (currentUser.getUser().getRegion() != null) {
			provinceFilter.setValue(currentUser.getUser().getRegion());
			filteredDataProvider.setFilter(criteria.region(currentUser.getUser().getRegion()));
//			criteria.region(currentUser.getUser().getRegion());
			provinceFilter.setEnabled(false);
		}

		layout.add(provinceFilter);

		regionFilter.addValueChangeListener(e -> {

			AreaReferenceDto area = e.getValue();
			criteria.area(area);
			provinceFilter.setItems(FacadeProvider.getRegionFacade().getAllActiveByArea(e.getValue().getUuid()));

			filteredDataProvider.setFilter(criteria);
			resetFilters.setVisible(true);
		});

		provinceFilter.addValueChangeListener(e -> {
			filteredDataProvider.setFilter(criteria);
			RegionReferenceDto province = e.getValue();
			criteria.region(province);
			filteredDataProvider.refreshAll();
		});

		searchField.addClassName("filterBar");
		searchField.setPlaceholder("Search");
		Icon searchIcon = new Icon(VaadinIcon.SEARCH);
		searchIcon.getStyle().set("color", "#0D6938");
		searchField.setPrefixComponent(searchIcon);
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		searchField.setWidth("25%");
		searchField.addValueChangeListener(e -> {
			criteria.nameEpidLike(e.getValue());// nameLike(e.getValue());
			filteredDataProvider.setFilter(criteria);
			resetFilters.setVisible(true);
		});

		resetFilters.addClassName("resetButton");
		resetFilters.setVisible(false);
		layout.add(resetFilters);
		resetFilters.addClickListener(e -> {

		});


		riskFilter.setItems("Low Risk (LR)", "Medium Risk (MR)", "High Risk (HR)");

		riskFilter.addValueChangeListener(e -> {

			if (e.getValue() != null) {
				criteria.risk(e.getValue().toString());
				filteredDataProvider.setFilter(criteria.risk(e.getValue().toString()));
			} else {
				criteria.risk(null);
			}
			
		});
		layout.add(riskFilter);

		
		relevanceStatusFilter.setItems(EntityRelevanceStatus.values());

		relevanceStatusFilter.setItemLabelGenerator(status -> {
			if (status == EntityRelevanceStatus.ARCHIVED) {
				return I18nProperties.getCaption(Captions.districtArchivedDistricts);
			} else if (status == EntityRelevanceStatus.ACTIVE) {
				return I18nProperties.getCaption(Captions.districtActiveDistricts);
			} else if (status == EntityRelevanceStatus.ALL) {
				return I18nProperties.getCaption(Captions.districtAllDistricts);
			}
			// Handle other enum values if needed
			return status.toString();
		});

		relevanceStatusFilter.addValueChangeListener(e -> {
			criteria.relevanceStatus((EntityRelevanceStatus) e.getValue());
			filteredDataProvider.setFilter(criteria.relevanceStatus((EntityRelevanceStatus) e.getValue()));
		});

		layout.add(relevanceStatusFilter);

		vlayout.add(displayFilters, layout);
		add(vlayout);
		return vlayout;
	}

	public void clearFilters() {

	}

}