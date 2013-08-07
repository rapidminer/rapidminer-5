Summary: RapidMiner %{_version-from-ant} Community Edition
Name: rapidminer
Version: %{_version-from-ant}
Release: 1
License: AGPL
Group: Applications
BuildRoot: %{_builddir}/%{name}-root
URL: http://rapid-i.com/
Vendor: Rapid-I GMBH
Packager: skirzynski@rapid-i.com
Prefix: /usr/share
Source:%{name}-%{version}.tar.gz
BuildArchitectures: noarch

%description
RapidMiner is the world-wide leading open-source data mining solution due to the combination of its leading-edge technologies and its functional range. Applications of RapidMiner cover a wide range of real-world data mining tasks. 

%prep
%setup -q
%build

%install

rm -rf $RPM_BUILD_ROOT

mkdir -p $RPM_BUILD_ROOT/usr/bin/
mkdir -p $RPM_BUILD_ROOT/usr/share/rapidminer

mv rapidminer $RPM_BUILD_ROOT/usr/bin/.
mv rapidminer-gui $RPM_BUILD_ROOT/usr/bin/.

# menu
install -dm 755 $RPM_BUILD_ROOT/usr/share/applications
cat > $RPM_BUILD_ROOT/usr/share/applications/%{name}.desktop <<EOF
[Desktop Entry]
Encoding=UTF-8
Type=Application
Categories=Education;Science;
Exec=rapidminer-gui
Icon=/usr/share/rapidminer/rapidminer.png
Terminal=0
Name=%{name} %{version}
Comment=%{summary}
EOF

cp -r * $RPM_BUILD_ROOT/usr/share/rapidminer

#pushd $RPM_BUILD_ROOT/usr/bin
#ln -s /usr/share/rapidminer/scripts/RapidMinerGUI rapidminer-gui
#popd

%clean
rm -rf *
%files
%attr(755,root,root) /usr/bin/rapidminer
%attr(755,root,root) /usr/bin/rapidminer-gui
%attr(755,root,root) /usr/share/rapidminer/scripts/rapidminer
%attr(755,root,root) /usr/share/rapidminer/scripts/RapidMinerGUI
/usr/share/applications/%{name}.desktop
/usr/share/rapidminer/rapidminer.png
/usr/share/rapidminer/lib
/usr/share/rapidminer/README
/usr/share/rapidminer/LICENSE
%changelog
* Tue Oct 20 2009 Skirzynski
- Created initial spec file
