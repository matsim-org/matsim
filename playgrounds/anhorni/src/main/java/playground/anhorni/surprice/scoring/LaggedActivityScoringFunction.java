/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.anhorni.surprice.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunction;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

public class LaggedActivityScoringFunction extends CharyparNagelOpenTimesScoringFunction {
		
	public LaggedActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, ActivityFacilities facilities) {
		super(plan, params, facilities);
		super.reset();
	}
}
