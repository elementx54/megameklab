/*
 * MegaMekLab - Copyright (C) 2010
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megameklab.com.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.FixedWingSupport;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SmallCraft;

public class ImageHelperAero {
    private static final String[] LOCATION_ABBRS =
        { "N", "LW", "RW", "A", "WG", "BD" };

    public static String getLocationAbbrs(int pos) {
        return LOCATION_ABBRS[pos];
    }

    public static void drawAeroArmorPip(Graphics2D g2d, float width, float height) {
        ImageHelperAero.drawAeroArmorPip(g2d, width, height, 9.0f);
    }

    public static void drawAeroArmorPip(Graphics2D g2d, float width, float height, float fontsize) {
        Font font = new Font("Arial", Font.PLAIN, 9);
        font = font.deriveFont(fontsize);
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);
        g2d.setBackground(Color.WHITE);
        g2d.drawString("O", width, height);
    }

    public static void drawAeroISPip(Graphics2D g2d, int width, int height) {
        Dimension circle = new Dimension(6, 6);
        Dimension fillCircle = new Dimension(4, 4);
        g2d.setColor(Color.black);
        g2d.fillOval(width, height, circle.width, circle.height);
        g2d.setColor(Color.white);
        g2d.fillOval(width + 1, height + 1, fillCircle.width, fillCircle.height);
    }

    public static void printAeroFuel(Aero aero, Graphics2D g2d) {
        int pointY = 330;
        int pointX = 22;
        String fuel = "Fuel: ";

        g2d.setFont(UnitUtil.getNewFont(g2d, fuel, true, 200, 7.0f));
        g2d.drawString(fuel, pointX, pointY);
        pointX += ImageHelper.getStringWidth(g2d, fuel, g2d.getFont());

        String fuelAmount = String.format("%1$s Points", aero.getFuel());
        g2d.setFont(UnitUtil.getNewFont(g2d, fuelAmount, false, 200, 7.0f));
        g2d.drawString(fuelAmount, pointX, pointY);
    }

    public static void printAeroWeaponsNEquipment(Aero aero, Graphics2D g2d) {
        int qtyPoint = 26;
        int typePoint = 38;
        int locPoint = 109;
        int heatPoint = 133;
        int shtPoint = 151;
        int medPoint = 169;
        int longPoint = 192;
        int erPoint = 211;
        float linePoint = 204f;

        float lineFeed = 6.7f;

        boolean newLineNeeded = false;

        ArrayList<Hashtable<String, EquipmentInfo>> equipmentLocations = new ArrayList<Hashtable<String, EquipmentInfo>>(aero.locations());

        for (int pos = 0; pos <= aero.locations(); pos++) {
            equipmentLocations.add(pos, new Hashtable<String, EquipmentInfo>());
        }

        for (Mounted eq : aero.getEquipment()) {

            if ((eq.isWeaponGroup() || (eq.getType() instanceof AmmoType)) || (eq.getLocation() == Entity.LOC_NONE) || !UnitUtil.isPrintableEquipment(eq.getType())) {
                continue;
            }

            Hashtable<String, EquipmentInfo> eqHash = equipmentLocations.get(eq.getLocation());

            String equipmentName = eq.getName();
            if (eq.isRearMounted()) {
                equipmentName += "(R)";
            }

            if (eqHash.containsKey(equipmentName)) {
                EquipmentInfo eqi = eqHash.get(equipmentName);

                if (eq.getType().getTechLevel() != eqi.techLevel) {
                    eqi = new EquipmentInfo(aero, eq);
                } else {
                    eqi.count++;
                }
                eqHash.put(equipmentName, eqi);
            } else {
                EquipmentInfo eqi = new EquipmentInfo(aero, eq);
                eqHash.put(equipmentName, eqi);
            }

        }

        Font font = UnitUtil.deriveFont(true, 10.0f);
        g2d.setFont(font);

        for (int pos = Aero.LOC_NOSE; pos <= aero.locations(); pos++) {

            Hashtable<String, EquipmentInfo> eqHash = equipmentLocations.get(pos);

            if (eqHash.size() < 1) {
                continue;
            }

            ArrayList<EquipmentInfo> equipmentList = new ArrayList<EquipmentInfo>();

            for (EquipmentInfo eqi : eqHash.values()) {
                equipmentList.add(eqi);

            }

            Collections.sort(equipmentList, StringUtils.equipmentInfoComparator());

            for (EquipmentInfo eqi : equipmentList) {
                newLineNeeded = false;

                font = UnitUtil.deriveFont(7.0f);
                g2d.setFont(font);

                g2d.drawString(Integer.toString(eqi.count), qtyPoint, linePoint);
                String name = eqi.name.trim() + " " + eqi.damage.trim();
                if (ImageHelper.getStringWidth(g2d, name, font) > 65) {
                    g2d.setFont(UnitUtil.getNewFont(g2d, eqi.name.trim(), false, 65, 7.0f));
                }

                if (eqi.c3Level == EquipmentInfo.C3I) {
                    ImageHelper.printC3iName(g2d, typePoint, linePoint, font, false);
                } else if (eqi.c3Level == EquipmentInfo.C3S) {
                    ImageHelper.printC3sName(g2d, typePoint, linePoint, font, false);
                } else if (eqi.c3Level == EquipmentInfo.C3M) {
                    ImageHelper.printC3mName(g2d, typePoint, linePoint, font, false);
                } else if (eqi.c3Level == EquipmentInfo.C3SB) {
                    ImageHelper.printC3sbName(g2d, typePoint, linePoint, font, false);
                } else if (eqi.c3Level == EquipmentInfo.C3MB) {
                    ImageHelper.printC3mbName(g2d, typePoint, linePoint, font, false);
                } else if (eqi.isMashCore) {
                    ImageHelper.printMashCore(g2d, typePoint, linePoint, font, false, aero);
                } else if (eqi.isDroneControl) {
                    ImageHelper.printDroneControl(g2d, typePoint, linePoint, font, false, aero);
                } else {
                    if (ImageHelper.getStringWidth(g2d, name, font) > 65) {
                        g2d.setFont(UnitUtil.getNewFont(g2d, eqi.name.trim(), false, 65, 7.0f));
                        g2d.drawString(eqi.name.trim(), typePoint, linePoint);
                        linePoint += lineFeed;
                        g2d.drawString(eqi.damage.trim(), typePoint, linePoint);
                    } else {
                        g2d.drawString(name, typePoint, linePoint);
                    }

                }
                font = UnitUtil.deriveFont(7.0f);
                g2d.setFont(font);

                String location = ImageHelperAero.getLocationAbbrs(pos);

                g2d.drawString(location, locPoint, linePoint);
                ImageHelper.printCenterString(g2d, Integer.toString(eqi.heat), font, heatPoint, linePoint);
                if (eqi.shtRange > 0) {
                    g2d.drawString(Integer.toString(eqi.shtRange), shtPoint, (int) linePoint);
                } else {
                    g2d.drawLine(shtPoint, (int) linePoint - 2, shtPoint + 6, (int) linePoint - 2);
                }

                if (eqi.medRange > 0) {
                    g2d.drawString(Integer.toString(eqi.medRange), medPoint, (int) linePoint);
                } else {
                    g2d.drawLine(medPoint, (int) linePoint - 2, medPoint + 6, (int) linePoint - 2);
                }
                if (eqi.longRange > 0) {
                    g2d.drawString(Integer.toString(eqi.longRange), longPoint, (int) linePoint);
                } else {
                    g2d.drawLine(longPoint, (int) linePoint - 2, longPoint + 6, (int) linePoint - 2);
                }
                if (eqi.erRange > 0) {
                    g2d.drawString(Integer.toString(eqi.erRange), erPoint, (int) linePoint);
                } else {
                    g2d.drawLine(erPoint, (int) linePoint - 2, erPoint + 6, (int) linePoint - 2);
                }

                if (eqi.hasArtemis) {
                    g2d.drawString("w/Artemis IV FCS", typePoint, linePoint + lineFeed);
                    newLineNeeded = true;
                } else if (eqi.hasArtemisV) {
                    g2d.drawString("w/Artemis V FCS", typePoint, linePoint + lineFeed);
                    newLineNeeded = true;
                } else if (eqi.hasApollo) {
                    g2d.drawString("w/Apollo FCS", typePoint, linePoint + lineFeed);
                    newLineNeeded = true;
                }

                linePoint += lineFeed;
                if (newLineNeeded) {
                    linePoint += lineFeed;
                }
            }
        }
        if (aero instanceof FixedWingSupport) {
            ImageHelperAero.printFixedWingSupportCargoChassisMod((FixedWingSupport) aero, g2d, (int) linePoint);
        }

        if (aero instanceof SmallCraft) {
            ImageHelperAero.printCargo(aero, g2d, (int) linePoint);
        }
        ImageHelper.printVehicleAmmo(aero, g2d, -18);
        ImageHelperAero.printAeroFuel(aero, g2d);
    }

    static public void printArmorPoints(Graphics2D g2d, Vector<float[]> pipPlotter, float totalArmor, float maxArmor) {
        pipPlotter.trimToSize();
        float pipSpace = maxArmor / totalArmor;
        for (float pos = 0; pos < pipPlotter.size(); pos += pipSpace) {
            int currentPip = (int) pos;
            ImageHelperAero.drawAeroArmorPip(g2d, pipPlotter.get(currentPip)[0], pipPlotter.get(currentPip)[1]);
            if (--totalArmor <= 0) {
                return;
            }
        }
    }

    public static void printFixedWingSupportCargoChassisMod(FixedWingSupport aero, Graphics2D g2d, int pointY) {
        int pointX = 22;
        double lineFeed = ImageHelper.getStringHeight(g2d, "H", g2d.getFont());

        if (aero.hasWorkingMisc(MiscType.F_CHASSIS_MODIFICATION)) {
            pointY += lineFeed;
            String chassisMods = "Chassis Modifications: ";
            for (Mounted misc : aero.getMisc()) {
                if (misc.getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION)) {
                    chassisMods += misc.getName() + ", ";
                }
            }
            chassisMods = chassisMods.substring(0, chassisMods.length() - 2);
            g2d.setFont(UnitUtil.getNewFont(g2d, chassisMods, false, 205, 7.0f));
            g2d.drawString(chassisMods, pointX, pointY);
        }
        printCargo(aero, g2d, pointY);
    }


    public static void printCargo(Aero aero, Graphics2D g2d, int pointY) {

        if ((aero.getTransportBays().size() < 1) && (aero.getTroopCarryingSpace() == 0)) {
            return;
        }

        int pointX = 22;
        double lineFeed = ImageHelper.getStringHeight(g2d, "H", g2d.getFont());

        Font font = UnitUtil.deriveFont(true, g2d.getFont().getSize2D());

        g2d.setFont(font);
        pointY += lineFeed;
        g2d.drawString("Cargo: ", pointX, pointY);

        font = UnitUtil.deriveFont(g2d.getFont().getSize2D());
        g2d.setFont(font);

        float troopspace = aero.getTroopCarryingSpace();
        if (troopspace > 0) {
            pointY += lineFeed;
            String troopString = "Infantry (";
            if (troopspace - Math.floor(troopspace) > 0) {
                troopString += String.valueOf(troopspace);
            } else {
                troopString += String.valueOf((int) troopspace);
            }
            if (troopspace == 1) {
                troopString += " ton)";
            } else {
                troopString += " tons)";
            }
            g2d.drawString(troopString, pointX, pointY);
        }

        pointY += lineFeed;

        for (Bay bay : aero.getTransportBays()) {
            g2d.drawString(ImageHelperDropShip.getBayString(bay), pointX, pointY);
            pointY += lineFeed;
        }

    }

}