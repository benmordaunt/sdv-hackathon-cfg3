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
    https://raw.githubusercontent.com/benmordaunt/sdv-hackathon-cfg3/main/xencfg/ubuntu-xenguest.conf;sha256sum=6aeaf4cc65f112cdc12bcc0e0616328d3f0c0727fb985809201f0b64e4b36fdc\
    https://cloud-images.ubuntu.com/focal/20221005/focal-server-cloudimg-arm64.img;sha256sum=5e8b8814419325d05acb62827515a13a3ce4e8572ad0e510c63301ab1e86d9f2\
"

inherit allarch
inherit features_check
REQUIRED_DISTRO_FEATURES += "ewaol-virtualization"

DEPENDS += "dosfstools-native mtools-native coreutils-native"
RDEPENDS:${PN} += "edk2-firmware"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    CFG_NAME="ubuntu-xenguest.conf"
    DISK_NAME="focal-server-cloudimg-arm64.img"
    DISK_DST="${datadir}/guest-vms/ubuntu-xenguest/focal-server-cloudimg-arm64.img"
    DISK_DIRNAME=$(dirname ${DISK_DST})
    
    install -d ${D}${sysconfdir}/xen/auto
    install -Dm 0640 ${WORKDIR}/${CFG_NAME} ${D}${sysconfdir}/xen/auto/${CFG_NAME}

    install -d ${D}${DISK_DIRNAME}
    install -Dm 0640 ${WORKDIR}/${DISK_NAME} ${D}${DISK_DST}
    
    if [ "${HKT_UBUNTU_NOCLOUD}" = "True" ]; then
        SEED_DST=${DISK_DIRNAME}/focal-server-cloudimg-arm64-seed.img

        printf "instance-id: bcx22-ubuntu\nlocal-hostname: bcx22-ubuntu\n" > ${WORKDIR}/meta-data
	printf "#cloud-config\npassword: bcx22\nchpasswd: { expire: False }\nssh_pwauth: True\n" > ${WORKDIR}/user-data

        truncate --size 2M ${WORKDIR}/seed.img
	mkfs.vfat -n cidata ${WORKDIR}/seed.img

	mcopy -oi ${WORKDIR}/seed.img ${WORKDIR}/user-data ${WORKDIR}/meta-data ::/

	install -Dm 0640 ${WORKDIR}/seed.img ${D}${SEED_DST}

	sed -i "s/%%HKT_UBUNTU_NOCLOUD_SEED_IMAGE%%/, 'format=raw, vdev=xvdb, access=rw, target=\/usr\/share\/guest-vms\/ubuntu-xenguest\/focal-server-cloudimg-arm64-seed.img'/" ${D}${sysconfdir}/xen/auto/${CFG_NAME}
    else
        sed -i "s/%%HKT_UBUNTU_NOCLOUD_SEED_IMAGE%%//" ${D}${sysconfdir}/xen/auto/${CFG_NAME}
    fi
}

FILES:${PN} = "${datadir} ${sysconfdir}"
