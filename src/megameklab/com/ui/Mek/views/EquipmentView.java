/*
 * MegaMekLab - Copyright (C) 2008
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package megameklab.com.ui.Mek.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megameklab.com.util.CriticalTableModel;
import megameklab.com.util.IView;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.StringUtils;
import megameklab.com.util.UnitUtil;

public class EquipmentView extends IView implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 799195356642563937L;

    private RefreshListener refresh;

    private JPanel mainPanel = new JPanel();
    private JPanel topPanel = new JPanel();
    private JPanel rightPanel = new JPanel();
    private JPanel buttonPanel = new JPanel();

    private JButton addButton = new JButton("Add");
    private JButton removeButton = new JButton("Remove");
    private JButton removeAllButton = new JButton("Remove All");

    private JComboBox equipmentCombo = new JComboBox();
    private CriticalTableModel equipmentList;
    private Vector<EquipmentType> masterEquipmentList = new Vector<EquipmentType>(10, 1);
    private JTable equipmentTable = new JTable();
    private JScrollPane equipmentScroll = new JScrollPane();
    private Vector<EquipmentType> equipmentTypes;

    private String ADD_COMMAND = "ADD";
    private String REMOVE_COMMAND = "REMOVE";
    private String REMOVEALL_COMMAND = "REMOVEALL";

    private int jumpBoosterMP = 0;

    public EquipmentView(Mech unit) {
        super(unit);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        topPanel.setBorder(BorderFactory.createEtchedBorder(Color.WHITE.brighter(), Color.blue.darker()));
        rightPanel.setBorder(BorderFactory.createEtchedBorder(Color.WHITE.brighter(), Color.blue.darker()));

        equipmentList = new CriticalTableModel(unit, CriticalTableModel.EQUIPMENT);

        equipmentTable.setModel(equipmentList);
        equipmentList.initColumnSizes(equipmentTable);
        for (int i = 0; i < equipmentList.getColumnCount(); i++) {
            equipmentTable.getColumnModel().getColumn(i).setCellRenderer(equipmentList.getRenderer());
        }

        equipmentTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        // equipmentScroll.setToolTipText("");
        equipmentScroll.setPreferredSize(new Dimension(getWidth() / 2, getHeight() * 3 / 4));
        equipmentTable.setDoubleBuffered(true);
        equipmentScroll.setViewportView(equipmentTable);

        topPanel.add(equipmentCombo);
        topPanel.add(addButton);

        buttonPanel.add(removeButton);
        buttonPanel.add(removeAllButton);

        rightPanel.add(topPanel);
        rightPanel.add(equipmentScroll);
        rightPanel.add(buttonPanel);

        mainPanel.add(rightPanel);

        Enumeration<EquipmentType> miscTypes = EquipmentType.getAllTypes();
        while (miscTypes.hasMoreElements()) {
            EquipmentType eq = miscTypes.nextElement();

            if (UnitUtil.isMechEquipment(eq)) {
                masterEquipmentList.add(eq);
            }
        }

        Collections.sort(masterEquipmentList, StringUtils.equipmentTypeComparator());
        this.add(mainPanel);
        loadEquipmentTable();

        addButton.setMnemonic('A');
        removeButton.setMnemonic('R');
        removeAllButton.setMnemonic('l');
    }

    public void addRefreshedListener(RefreshListener l) {
        refresh = l;
    }

    private void loadEquipmentCombo() {

        equipmentCombo.removeAllItems();
        equipmentTypes = new Vector<EquipmentType>();

        for (EquipmentType eq : masterEquipmentList) {
            if (UnitUtil.isLegal(unit, eq.getTechLevel())) {
                equipmentTypes.add(eq);
                equipmentCombo.addItem(UnitUtil.getCritName(unit, eq));
            }
        }
    }

    private void loadEquipmentTable() {
        List<EquipmentType> spreadAlreadyAdded = new ArrayList<EquipmentType>();
        for (Mounted mount : unit.getMisc()) {

            if ((mount.getType().hasFlag(MiscType.F_HEAT_SINK) || mount.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK) || mount.getType().hasFlag(MiscType.F_LASER_HEAT_SINK) || UnitUtil.isArmorOrStructure(mount.getType()))) {
                continue;
            }
            if (UnitUtil.isMechEquipment(mount.getType())) {
                if (UnitUtil.isSpreadEquipment(mount.getType()) && !spreadAlreadyAdded.contains(mount.getType())) {
                    equipmentList.addCrit(mount.getType());
                    // keep track of spreadable equipment here, so it doesn't
                    // show up multiple times in the table
                    spreadAlreadyAdded.add(mount.getType());
                }
            }
        }
    }

    private void loadHeatSinks() {
        int engineHeatSinks = UnitUtil.getBaseChassisHeatSinks(getMech());
        for (Mounted mount : unit.getMisc()) {

            if ((mount.getType().hasFlag(MiscType.F_HEAT_SINK) || mount.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK) || mount.getType().hasFlag(MiscType.F_LASER_HEAT_SINK))) {
                if (engineHeatSinks-- > 0) {
                    continue;
                }
                equipmentList.addCrit(mount.getType());
            }
        }

    }

    private void removeHeatSinks() {
        int location = 0;
        for (; location < equipmentList.getRowCount();) {

            EquipmentType eq = (EquipmentType) equipmentList.getValueAt(location, CriticalTableModel.EQUIPMENT);
            if ((eq instanceof MiscType) && ((eq.hasFlag(MiscType.F_HEAT_SINK) || eq.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) || eq.hasFlag(MiscType.F_LASER_HEAT_SINK)))) {
                try {
                    equipmentList.removeCrit(location);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                location++;
            }
        }
    }

    public void setJumpBoosterMP(int mp) {
        jumpBoosterMP = mp;
    }

    public void refresh() {
        removeAllListeners();
        loadEquipmentCombo();
        updateEquipment();
        addAllListeners();
        fireTableRefresh();
    }

    private void removeAllListeners() {
        addButton.removeActionListener(this);
        removeButton.removeActionListener(this);
        removeAllButton.removeActionListener(this);
    }

    private void addAllListeners() {
        addButton.addActionListener(this);
        removeButton.addActionListener(this);
        removeAllButton.addActionListener(this);
        addButton.setActionCommand(ADD_COMMAND);
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeAllButton.setActionCommand(REMOVEALL_COMMAND);
        addButton.setMnemonic('A');
        removeButton.setMnemonic('R');
        removeAllButton.setMnemonic('L');
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand().equals(ADD_COMMAND)) {
            boolean success = false;
            String equip = equipmentCombo.getSelectedItem().toString();
            if ((equipmentTypes.elementAt(equipmentCombo.getSelectedIndex()).getTechLevel() != TechConstants.T_ALLOWED_ALL)
                    && (equipmentTypes.elementAt(equipmentCombo.getSelectedIndex()).getTechLevel() != TechConstants.T_TECH_UNKNOWN)) {
                if (unit.isClan() && !UnitUtil.isClanEquipment(equipmentTypes.elementAt(equipmentCombo.getSelectedIndex()))) {
                    equip = equip.substring(0, equip.length()-5);
                } else if (!unit.isClan() && UnitUtil.isClanEquipment(equipmentTypes.elementAt(equipmentCombo.getSelectedIndex()))) {
                    equip = equip.substring(0, equip.length()-7);
                }
            }
            if (equip.equals(UnitUtil.TSM)) {
                if (!getMech().hasTSM()) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.TSM);
                }
            } else if (equip.equals(UnitUtil.INDUSTRIALTSM)) {
                if (!getMech().hasIndustrialTSM()) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.INDUSTRIALTSM);
                }
            } else if (equip.equals(UnitUtil.ENVIROSEAL)) {
                if (!unit.hasEnvironmentalSealing()) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.ENVIROSEAL);
                }
            } else if (equip.equals(UnitUtil.NULLSIG)) {
                if (!getMech().hasNullSig()) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.NULLSIG);
                }
            } else if (equip.equals(UnitUtil.VOIDSIG)) {
                if (!getMech().hasVoidSig()) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.VOIDSIG);
                }
            } else if (equip.equals(UnitUtil.TRACKS)) {
                if (!getMech().hasTracks()) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.TRACKS);
                }
            } else if (equip.equals(UnitUtil.PARTIALWING)) {
                if (!getMech().hasWorkingMisc(UnitUtil.PARTIALWING)) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.PARTIALWING);
                }
            } else if (equip.equals(UnitUtil.JUMPBOOSTER)) {
                setJumpBoosterMP(Integer.parseInt(JOptionPane.showInputDialog(this, "How many Jump MP?")));
                updateJumpMP();
                if (!getMech().hasWorkingMisc(MiscType.F_JUMP_BOOSTER)) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.JUMPBOOSTER);
                }
            } else if (equip.equals(UnitUtil.TALONS)) {
                boolean hasTalons = getMech().hasWorkingMisc(MiscType.F_TALON);
                if (!hasTalons) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.TALONS);
                }
            } else if (equip.startsWith(UnitUtil.TARGETINGCOMPUTER)) {
                if (!UnitUtil.hasTargComp(unit)) {

                    boolean isClan = false;
                    if (unit.isMixedTech()) {
                        String tcType = equip.trim();
                        if ((tcType.endsWith("(Clan)") && !unit.isClan()) || (unit.isClan() && !tcType.endsWith("(IS)"))) {
                            isClan = true;
                        }
                    }
                    UnitUtil.updateTC(getMech(), isClan);
                    success = true;
                }
            } else if (equip.equals(UnitUtil.CHAMELEON)) {
                if (!getMech().hasChameleonShield()) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.CHAMELEON);
                }
            } else if (equip.equals(UnitUtil.BLUESHIELD)) {
                if (!unit.hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
                    success = UnitUtil.createSpreadMounts(getMech(), UnitUtil.BLUESHIELD);
                }
            } else {
                try {
                    getMech().addEquipment(new Mounted(unit, equipmentTypes.elementAt(equipmentCombo.getSelectedIndex())), Entity.LOC_NONE, false);
                    success = true;
                } catch (LocationFullException lfe) {
                    // this can't happen, we add to Entity.LOC_NONE
                }
            }
            if (success) {
                equipmentList.addCrit(equipmentTypes.elementAt(equipmentCombo.getSelectedIndex()));
            }
        } else if (e.getActionCommand().equals(REMOVE_COMMAND)) {
            int startRow = equipmentTable.getSelectedRow();
            int count = equipmentTable.getSelectedRowCount();

            for (; count > 0; count--) {
                if (startRow > -1) {
                    if (((EquipmentType)equipmentList.getValueAt(startRow, CriticalTableModel.EQUIPMENT)).getName().equals(UnitUtil.JUMPBOOSTER)) {
                        setJumpBoosterMP(0);
                    }
                    equipmentList.removeMounted(startRow);
                    equipmentList.removeCrit(startRow);
                }
            }
        } else if (e.getActionCommand().equals(REMOVEALL_COMMAND)) {
            removeAllEquipment();
        } else {
            return;
        }
        fireTableRefresh();
    }

    public void updateEquipment() {
        removeHeatSinks();
        equipmentList.removeAllCrits();
        loadHeatSinks();
        loadEquipmentTable();
    }

    public void removeAllEquipment() {
        removeHeatSinks();
        for (int count = 0; count < equipmentList.getRowCount(); count++) {
            equipmentList.removeMounted(count);
        }
        equipmentList.removeAllCrits();
        loadHeatSinks();
    }

    private void fireTableRefresh() {
        equipmentList.updateUnit(unit);
        equipmentList.refreshModel();
        equipmentScroll.setPreferredSize(new Dimension(getWidth() * 90 / 100, getHeight() * 8 / 10));
        equipmentScroll.repaint();
        updateJumpMP();
        if (refresh != null) {
            refresh.refreshStatus();
            refresh.refreshBuild();
        }
    }

    private void updateJumpMP() {
        int mp = 0;
        if (jumpBoosterMP > 0) {
            mp = jumpBoosterMP;
        } else {
            for (Mounted mount : unit.getEquipment()) {
                if (mount.getType() instanceof MiscType) {
                    if (mount.getType().hasFlag(MiscType.F_JUMP_JET)) {
                        mp++;
                    }
                }
            }
        }
        unit.setOriginalJumpMP(mp);
    }

    public CriticalTableModel getEquipmentList() {
        return equipmentList;
    }

}