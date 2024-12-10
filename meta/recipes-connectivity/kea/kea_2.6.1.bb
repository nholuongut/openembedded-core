SUMMARY = "ISC Kea DHCP Server"
DESCRIPTION = "Kea is the next generation of DHCP software developed by ISC. It supports both DHCPv4 and DHCPv6 protocols along with their extensions, e.g. prefix delegation and dynamic updates to DNS."
HOMEPAGE = "http://kea.isc.org"
SECTION = "connectivity"
LICENSE = "MPL-2.0"
LIC_FILES_CHKSUM = "file://COPYING;md5=618093ea9de92c70a115268c1d53421f"

DEPENDS = "boost log4cplus openssl"

SRC_URI = "http://ftp.isc.org/isc/kea/${PV}/${BP}.tar.gz \
           file://kea-dhcp4.service \
           file://kea-dhcp6.service \
           file://kea-dhcp-ddns.service \
           file://kea-dhcp4-server \
           file://kea-dhcp6-server \
           file://kea-dhcp-ddns-server \
           file://fix-multilib-conflict.patch \
           file://fix_pid_keactrl.patch \
           file://0001-src-lib-log-logger_unittest_support.cc-do-not-write-.patch \
           file://0001-Replace-Name-NameString-with-vector-of-uint8_t.patch \
           file://0002-Fix-unittests-Typo-in-Name-Name-append-to-ndata_-not.patch \
           "
SRC_URI[sha256sum] = "d2ce14a91c2e248ad2876e29152d647bcc5e433bc68dafad0ee96ec166fcfad1"

inherit autotools systemd update-rc.d upstream-version-is-even

INITSCRIPT_NAME = "kea-dhcp4-server"
INITSCRIPT_PARAMS = "defaults 30"

SYSTEMD_SERVICE:${PN} = "kea-dhcp4.service kea-dhcp6.service kea-dhcp-ddns.service"
SYSTEMD_AUTO_ENABLE = "disable"

DEBUG_OPTIMIZATION:remove:mips = " -Og"
DEBUG_OPTIMIZATION:append:mips = " -O"
BUILD_OPTIMIZATION:remove:mips = " -Og"
BUILD_OPTIMIZATION:append:mips = " -O"

DEBUG_OPTIMIZATION:remove:mipsel = " -Og"
DEBUG_OPTIMIZATION:append:mipsel = " -O"
BUILD_OPTIMIZATION:remove:mipsel = " -Og"
BUILD_OPTIMIZATION:append:mipsel = " -O"

CXXFLAGS:remove = "-fvisibility-inlines-hidden"
EXTRA_OECONF = "--with-boost-libs=-lboost_system \
                --with-log4cplus=${STAGING_DIR_TARGET}${prefix} \
                --with-openssl=${STAGING_DIR_TARGET}${prefix}"

do_configure:prepend() {
    # replace abs_top_builddir to avoid introducing the build path
    # don't expand the abs_top_builddir on the target as the abs_top_builddir is meanlingless on the target
    find ${S} -type f -name *.sh.in | xargs sed -i  "s:@abs_top_builddir@:@abs_top_builddir_placeholder@:g"
    sed -i "s:@abs_top_builddir@:@abs_top_builddir_placeholder@:g" ${S}/src/bin/admin/kea-admin.in
}

# patch out build host paths for reproducibility
do_compile:prepend:class-target() {
        sed -i -e "s,${WORKDIR},,g" ${B}/config.report
}

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${UNPACKDIR}/kea-dhcp*service ${D}${systemd_system_unitdir}
    install -m 0755 ${UNPACKDIR}/kea-*-server ${D}${sysconfdir}/init.d
    sed -i -e 's,@SBINDIR@,${sbindir},g' -e 's,@BASE_BINDIR@,${base_bindir},g' \
           -e 's,@LOCALSTATEDIR@,${localstatedir},g' -e 's,@SYSCONFDIR@,${sysconfdir},g' \
           ${D}${systemd_system_unitdir}/kea-dhcp*service ${D}${sbindir}/keactrl
    sed -i "s:${B}/../${BPN}-${PV}:@abs_top_builddir_placeholder@:g" ${D}${sbindir}/kea-admin
}

do_install:append() {
    rm -rf "${D}${localstatedir}"
}

CONFFILES:${PN} = "${sysconfdir}/kea/keactrl.conf"

FILES:${PN}-staticdev += "${libdir}/kea/hooks/*.a ${libdir}/hooks/*.a"
FILES:${PN} += "${libdir}/hooks/*.so"

PARALLEL_MAKEINST = ""
