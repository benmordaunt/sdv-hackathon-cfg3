# This recipe makes up for a deficiency in the main `prebuilt-guest-vm-package` recipe,
# which is only really geared for importing pre-built Linux distributions generated by Yocto.
#
# Namely, the prebuilt-guest-vm-package will always fail when there is not a separate kernel image
# and rootfs. However, in many "cloud bundle"-style images, such as those for Ubuntu by Canonical,
# the kernel is bundled directly under /boot in the rootfs.
#
# Somewhat confusingly, it's easiest to bypass EWAOL's VM management completely, and just install
# a Xen configuration to ${D}${sysconfdir}/xen/auto

SUMMARY = "This recipe provides an EWAOL Xen Guest VM configuration for Ubuntu 20.04.5 LTS"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "\
    file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302 \
    "

SRC_URI:append = " \
    https://raw.githubusercontent.com/benmordaunt/sdv-hackathon-cfg3/main/xencfg/ubuntu-xenguest.conf;sha256sum=05a83b586e5dc4e538eb8d3517ef6c4fd4135860d1a9cbff6d3f4af7554fe065\
    https://github.com/benmordaunt/sdv-hackathon-cfg3/raw/main/prebuilt/images/focal-server-cloudimg-arm64.raw.img.bz2;sha256sum=c6c0bd96f291864d50681d9fc34a4766b48ac2902e3d383525e0e88eb25c8964\
"

inherit allarch
inherit features_check
REQUIRED_DISTRO_FEATURES += "ewaol-virtualization"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    CFG_NAME="ubuntu-xenguest.conf"
    DISK_NAME="focal-server-cloudimg-arm64.raw.img"
    DISK_DST="${datadir}/guest-vms/ubuntu-xenguest/focal-server-cloudimg-arm64.img"
    DISK_DIRNAME=$(dirname ${DISK_DST})

    install -d ${D}${sysconfdir}/xen/auto
    install -Dm 0640 ${WORKDIR}/${CFG_NAME} ${D}${sysconfdir}/xen/auto/${CFG_NAME}

    install -d ${D}${DISK_DIRNAME}
    install -Dm 0640 ${WORKDIR}/${DISK_NAME} ${D}${DISK_DST}
}

FILES:${PN} = "${datadir} ${sysconfdir}"
