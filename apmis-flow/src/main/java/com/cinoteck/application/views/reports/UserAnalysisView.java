package com.cinoteck.application.views.reports;

import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

import de.symeda.sormas.api.infrastructure.community.CommunityCriteriaNew;
import de.symeda.sormas.api.user.FormAccess;

@Route(layout = ReportView.class)
public class UserAnalysisView extends VerticalLayout implements RouterLayout {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8119024598018785898L;
	private Map<Tab, Component> userAnalysisComponentMap = new LinkedHashMap<>();
	
	
	
	
	private Tabs createUserAnalysisTabs() {
		
		//TODO: create filters here to cater for all tabs and passed to the body of the view classes
		CommunityCriteriaNew criteria = new CommunityCriteriaNew();
		
		FormAccess frms[] = FormAccess.values();
		for (FormAccess lopper : frms) {
			userAnalysisComponentMap.put(new Tab(lopper.toString()), new UserAnalysisGridView(criteria, lopper));
		}
		
		
		return new Tabs(userAnalysisComponentMap.keySet().toArray(new Tab[] {}));

	}
	
	public UserAnalysisView() {
		
		setSizeFull();
		HorizontalLayout reportTabsheetLayout = new HorizontalLayout();
		 reportTabsheetLayout.setClassName("campDatFill");
		 
		 Tabs tabs = createUserAnalysisTabs();
			tabs.getStyle().set("background", "#434343");
	        tabs.getStyle().set("width", "100%");
	        Div contentContainer = new Div();
	        contentContainer.setSizeFull();
	        
	        tabs.addSelectedChangeListener(e -> {
	            contentContainer.removeAll();
	            contentContainer.add(userAnalysisComponentMap.get(e.getSelectedTab()));
	        });
	        
	        // Set initial content
	        contentContainer.add(userAnalysisComponentMap.get(tabs.getSelectedTab()));
	        reportTabsheetLayout.add(tabs);
	        add(reportTabsheetLayout, contentContainer);

	}

}
