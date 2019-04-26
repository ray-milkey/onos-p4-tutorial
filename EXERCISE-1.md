Session 1: Software tools basics

Hands on introduction to software tools used in this tutorial. 

Participants will
be provided with a VM including a starter P4 program, PTF-based unit tests,
skeleton ONOS app code, and Mininet script to emulate a fabric topology.

In
this session, students will be asked to apply changes to the P4 code and
ONOS app to support controller packet-in/out for link discovery.


The lesson
will progressively introduce concepts around the use of arbitrary P4 programs
with ONOS and how to re-use existing ONOS apps such as topology
discovery with custom P4 programs.

Students will also be introduced to using
the PTF framework to write Python-based unit tests for their P4 program.


By
the end of this session, students will be able to run ONOS controlling a 2x2
fabric topology of stratum-bmv2 devices with links automatically discovered.

-----

# Exercise 1

Hands on introduction to software tools used in this tutorial. Participants will
be provided with a VM including a starter P4 program, PTF-based unit tests,
skeleton ONOS app code, and Mininet script to emulate a fabric topology. In this
session, students will be asked to apply changes to the P4 code and ONOS app to
support controller packet-in/out for link discovery. The lesson will
progressively introduce concepts around the use of arbitrary P4 programs with
ONOS and how to re-use existing ONOS apps such as topology discovery with custom
P4 programs. Students will also be introduced to using the PTF framework to
write Python-based unit tests for their P4 program. By the end of this session,
students will be able to run ONOS controlling a 2x2 fabric topology of
stratum-bmv2 devices with links automatically discovered.

- Explain LLDP topology discovery, why we need packet-in/out?

- Packet-in/out in P4Runtime - custom metadata

- TODO: Modify packetio PTF test

- Explain Mapping of Metadata in ONOS (Interpreter)

- TODO Modify interpreter to map packet_ins

- Run ONOS with mininet and app to show that topology discovery works
    (use instructions from OLD tutorials)

-----

Steps:
1. Update P4 code?
2. Update ptf test 
3. Run ptf test
4. Update pipe