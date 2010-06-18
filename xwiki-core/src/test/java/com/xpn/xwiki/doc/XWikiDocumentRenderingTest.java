/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.doc;

import java.util.ArrayList;
import java.util.Arrays;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Unit tests for {@link XWikiDocument}.
 * 
 * @version $Id$
 */
public class XWikiDocumentRenderingTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String DOCWIKI = "xwiki";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    private static final String CLASSNAME = DOCFULLNAME;

    private XWikiDocument document;

    private XWikiDocument translatedDocument;

    private Mock mockXWiki;

    private Mock mockXWikiRenderingEngine;

    private Mock mockXWikiVersioningStore;

    private Mock mockXWikiStoreInterface;

    private Mock mockXWikiRightService;

    private BaseClass baseClass;

    private BaseObject baseObject;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(DOCWIKI, DOCSPACE, DOCNAME);
        this.document.setSyntaxId("xwiki/1.0");
        this.document.setLanguage("en");
        this.document.setDefaultLanguage("en");
        this.document.setNew(false);

        getContext().setDoc(this.document);

        this.translatedDocument = new XWikiDocument();
        this.translatedDocument.setSyntaxId("xwiki/1.0");
        this.translatedDocument.setLanguage("fr");
        this.translatedDocument.setNew(false);

        getContext().put("isInRenderingEngine", true);

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("Param").will(returnValue(null));

        this.mockXWikiRenderingEngine = mock(XWikiRenderingEngine.class);
        this.mockXWikiRenderingEngine.stubs().method("interpretText").will(
            new CustomStub("Implements XWikiRenderingEngine.interpretText")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return invocation.parameterValues.get(0);
                }
            });

        this.mockXWikiVersioningStore = mock(XWikiVersioningStoreInterface.class);
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));

        this.mockXWikiRightService = mock(XWikiRightService.class);
        this.mockXWikiRightService.stubs().method("hasProgrammingRights").will(returnValue(true));

        this.mockXWikiStoreInterface = mock(XWikiStoreInterface.class);
        this.mockXWikiStoreInterface.stubs().method("search").will(returnValue(new ArrayList<XWikiDocument>()));

        this.document.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        this.mockXWiki.stubs().method("getRenderingEngine").will(returnValue(this.mockXWikiRenderingEngine.proxy()));
        this.mockXWiki.stubs().method("getVersioningStore").will(returnValue(this.mockXWikiVersioningStore.proxy()));
        this.mockXWiki.stubs().method("getStore").will(returnValue(this.mockXWikiStoreInterface.proxy()));
        this.mockXWiki.stubs().method("getRightService").will(returnValue(this.mockXWikiRightService.proxy()));
        this.mockXWiki.stubs().method("getDocument").will(returnValue(this.document));
        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue("en"));
        this.mockXWiki.stubs().method("exists").will(returnValue(false));
        this.mockXWiki.stubs().method("ParamAsLong").will(returnValue(2L));

        getContext().setWiki((XWiki) this.mockXWiki.proxy());

        this.baseClass = this.document.getxWikiClass();
        this.baseClass.addTextField("string", "String", 30);
        this.baseClass.addTextAreaField("area", "Area", 10, 10);
        this.baseClass.addTextAreaField("puretextarea", "Pure text area", 10, 10);
        // set the text areas an non interpreted content
        ((TextAreaClass) this.baseClass.getField("puretextarea")).setContentType("puretext");
        this.baseClass.addPasswordField("passwd", "Password", 30);
        this.baseClass.addBooleanField("boolean", "Boolean", "yesno");
        this.baseClass.addNumberField("int", "Int", 10, "integer");
        this.baseClass.addStaticListField("stringlist", "StringList", "value1, value2");

        this.mockXWiki.stubs().method("getXClass").will(returnValue(this.baseClass));

        this.baseObject = this.document.newObject(CLASSNAME, getContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));
    }

    public void testCurrentDocumentVariableIsInjectedBeforeRendering() throws XWikiException
    {
        // Verifies we can access the doc variable from a groovy macro.
        this.document.setContent("{{groovy}}print(doc);{{/groovy}}");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("<p>Space.Page</p>", this.document.getRenderedContent(getContext()));
    }

    public void testGetRenderedTitleWithTitle() throws XWikiException
    {
        this.document.setSyntaxId("xwiki/2.0");

        this.document.setTitle("title");

        assertEquals("title", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setTitle("**title**");

        assertEquals("**title**", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setTitle("<strong>title</strong>");

        assertEquals("<strong>title</strong>", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
        assertEquals("title", this.document.getRenderedTitle(Syntax.PLAIN_1_0, getContext()));

        this.document.setTitle("#set($key = \"title\")$key");
        this.mockXWikiRenderingEngine.stubs().method("interpretText").with(eq("#set($key = \"title\")$key"), ANYTHING,
            ANYTHING).will(returnValue("title"));

        assertEquals("title", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    public void testGetRenderedTitleWithoutTitleHTML() throws XWikiException
    {
        this.document.setSyntaxId("xwiki/2.0");

        this.document.setContent("content not in section\n" + "= header 1=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("header 1", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent("content not in section\n" + "= **header 1**=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("<strong>header 1</strong>", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent("content not in section\n" + "= [[Space.Page]]=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        this.mockXWiki.stubs().method("getURL").will(returnValue("/reference"));

        assertEquals("<span class=\"wikicreatelink\"><a href=\"/reference\"><span class=\"wikigeneratedlinkcontent\">"
            + "Page" + "</span></a></span>", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent("content not in section\n" + "= #set($var ~= \"value\")=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("#set($var = \"value\")", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("value", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent("content not in section\n=== header 3===");

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    public void testGetRenderedTitleWithoutTitlePLAIN() throws XWikiException
    {
        this.document.setSyntaxId("xwiki/2.0");

        this.document.setContent("content not in section\n" + "= **header 1**=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("header 1", this.document.getRenderedTitle(Syntax.PLAIN_1_0, getContext()));

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("value", this.document.getRenderedTitle(Syntax.PLAIN_1_0, getContext()));
    }

    public void testGetRenderedTitleNoTitleAndContent() throws XWikiException
    {
        this.document.setSyntaxId("xwiki/2.0");

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    public void testExtractTitle()
    {
        this.document.setSyntaxId("xwiki/2.0");

        this.document.setContent("content not in section\n" + "= header 1=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("header 1", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= **header 1**=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("<strong>header 1</strong>", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= [[Space.Page]]=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        this.mockXWiki.stubs().method("getURL").will(returnValue("/reference"));

        assertEquals("<span class=\"wikicreatelink\"><a href=\"/reference\"><span class=\"wikigeneratedlinkcontent\">"
            + "Page" + "</span></a></span>", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= #set($var ~= \"value\")=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("#set($var = \"value\")", this.document.extractTitle());

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("value", this.document.extractTitle());

        this.document.setContent("content not in section\n=== header 3===");

        assertEquals("", this.document.extractTitle());
    }

    public void testExtractTitle10()
    {
        this.document.setContent("content not in section\n" + "1 header 1\nheader 1 content\n"
            + "1.1 header 2\nheader 2 content");

        assertEquals("header 1", this.document.extractTitle());

        this.document.setContent("content not in section\n");

        assertEquals("", this.document.extractTitle());
    }
}
